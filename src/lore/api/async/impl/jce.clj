(ns lore.api.async.impl.jce
  (:require [clojure.core.async :as async]
            [clojure.string :as string]
            [cognitect.transit :as transit]
            [lore.api.async :as lore]
            [lore.util :refer [->bytes error?]])
  (:import [java.security KeyStore KeyStore$PasswordProtection KeyStore$SecretKeyEntry SecureRandom]
           [java.io ByteArrayInputStream ByteArrayOutputStream]
           [javax.crypto Cipher KeyGenerator]
           [javax.crypto.spec IvParameterSpec]
           [java.util Arrays]))

; We include padding/unpadding functions here to allow use of
; PKCS11 Ciphers, which might not support native padding schemes.

(defmulti pad (fn [format _block-size _input-size] format))
(defmulti unpad (fn [format _input] format))

(defmethod pad :nopadding
  [_format _block-size _input-size]
  (byte-array []))

(defmethod pad :pkcs5
  [_format block-size input-size]
  (let [n (- block-size (mod input-size block-size))
        pad (byte-array n)]
    (Arrays/fill pad (.byteValue n))
    pad))

(defmethod unpad :nopadding
  [_format input]
  input)

(defmethod unpad :pkcs5
  [_format input]
  (let [n (bit-and (last input) 0xFF)
        b (byte-array (- (alength input) n))]
    (System/arraycopy input 0 b 0 (alength b))
    b))

(defrecord AsyncKeyStoreSecretStore [^KeyStore keystore provider master-key-password algorithm format padding]
  lore/IAsyncSecretStore
  (error? [_this result] (error? result))

  (decrypt [_this v]
    (let [chan (async/promise-chan)]
      (async/thread
        (async/put! chan
          (try
            (let [reader (transit/reader (ByteArrayInputStream. (->bytes v)) format)
                  {:keys [ciphertext iv key-id]} (transit/read reader)
                  key (let [e (.getEntry keystore key-id (some-> master-key-password (.toCharArray) (KeyStore$PasswordProtection.)))]
                        (if (instance? KeyStore$SecretKeyEntry e)
                          (.getSecretKey e)
                          (throw (ex-info (str "keystore entry with ID " key-id " is not a secret key") {}))))
                  cipher (doto (if (some? provider)
                                 (Cipher/getInstance algorithm provider)
                                 (Cipher/getInstance algorithm))
                           (.init Cipher/DECRYPT_MODE key (IvParameterSpec. iv)))
                  plaintext (unpad padding (.doFinal cipher ciphertext))]
              {:plaintext plaintext})
            (catch Exception e
              {:cognitect.anomalies/category :cognitect.anomalies/fault
               :cognitect.anomalies/message (.getMessage e)
               :exception e}))))
      chan)))

(defn- hex
  [b]
  (->> b
       (map #(format "%02x" %))
       (string/join)))

(defn- ^String cipher-alg
  [^String spec]
  (first (.split spec "/")))

(defn- random-bytes
  ([n] (random-bytes n (SecureRandom.)))
  ([n random]
   (let [b (byte-array n)]
     (.nextBytes random b)
     b)))

(defn- concat-bytes
  [b1 b2]
  (cond
    (zero? (alength b1)) b2
    (zero? (alength b2)) b1
    :else (let [b (byte-array (+ (alength b1) (alength b2)))]
            (System/arraycopy b1 0 b 0 (alength b1))
            (System/arraycopy b2 0 b (alength b1) (alength b2))
            b)))

(defn encrypt
  [^KeyStore keystore plaintext {:keys [key-id algorithm format iv generate-key? key-password key-size padding]
                                 :or {algorithm "AES/CBC/NoPadding"
                                      format :msgpack
                                      key-size 128
                                      padding :nopadding}}]
  (let [key (if-let [key (when (some? key-id)
                           (when-let [entry (.getEntry keystore key-id (some-> key-password (.toCharArray) (KeyStore$PasswordProtection.)))]
                             (when (instance? KeyStore$SecretKeyEntry entry)
                               (.getSecretKey ^KeyStore$SecretKeyEntry entry))))]
              key
              (if generate-key?
                (let [key-id (or key-id (-> (random-bytes 8) hex))
                      kg (doto (KeyGenerator/getInstance (cipher-alg algorithm) (.getProvider keystore))
                           (.init key-size))
                      key (.generateKey kg)]
                  (.setKeyEntry keystore key-id key key-password nil)
                  key)
                (throw (ex-info "no suitable secret key to encrypt" {:key-id key-id}))))
        cipher (Cipher/getInstance ^String algorithm (.getProvider keystore))
        iv (or iv (random-bytes (.getBlockSize cipher)))]
    (.init cipher Cipher/ENCRYPT_MODE key (IvParameterSpec. iv))
    (let [pt (->bytes plaintext)
          ct1 (.update cipher pt)
          ct2 (.doFinal cipher (pad padding (.getBlockSize cipher) (alength pt)))
          ciphertext (concat-bytes ct1 ct2)
          result {:ciphertext ciphertext :iv iv :key-id key-id}
          bout (ByteArrayOutputStream.)
          writer (transit/writer bout format)]
      (transit/write writer result)
      (.toByteArray bout))))

(defn- ->char-array
  [s]
  (cond
    (-> s class .getComponentType (= Character/TYPE)) s
    (string? s) (.toCharArray s)))

(defn ->secret-store
  [{:keys [store-type store-password algorithm key-password format padding]
    :or {algorithm "AES/CBC/NoPadding"
         format :msgpack
         padding :nopadding}}]
  (let [keystore (doto (KeyStore/getInstance store-type)
                   (.load nil (->char-array store-password)))]
    (->AsyncKeyStoreSecretStore keystore
                                (.getProvider keystore)
                                key-password
                                algorithm
                                format
                                padding)))