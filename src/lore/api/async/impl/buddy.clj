(ns lore.api.async.impl.buddy
  (:require [buddy.core.crypto :as crypto]
            [buddy.core.nonce :as nonce]
            lore.api.async
            [lore.util :refer :all]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.core.async :as async])
  (:import [java.io PushbackReader]
           [java.util Base64]))

(defrecord BuddySecretStore [master-key options]
  lore.api.async/IAsyncSecretStore
  (error? [_this v] (error? v))

  (decrypt [_this v]
    (let [chan (async/promise-chan)]
      (async/thread
        (try
          (let [{:keys [ciphertext iv]} (edn/read (PushbackReader. (io/reader (->bytes v))))
                ciphertext (.decode (Base64/getDecoder) ciphertext)
                iv (.decode (Base64/getDecoder) iv)
                plaintext (crypto/decrypt ciphertext master-key iv options)]
            (async/put! chan {:plaintext plaintext}))
          (catch Exception e
            (async/put! chan {:cognitect.anomalies/category :cognitect.anomalies/fault
                              :cognitect.anomalies/message (.getMessage e)
                              :exception e}))))
      chan)))

(defn encrypt
  "Function for encrypting a value with buddy, and formatting the output properly."
  ([key plaintext options] (encrypt key plaintext (nonce/random-bytes 16) options))
  ([key plaintext iv options]
   (-> {:ciphertext (.encodeToString (Base64/getEncoder) (crypto/encrypt plaintext key iv options))
        :iv (.encodeToString (Base64/getEncoder) iv)}
       (pr-str)
       (.getBytes))))

(defn ->buddy-store
  [arg-map]
  (map->BuddySecretStore arg-map))