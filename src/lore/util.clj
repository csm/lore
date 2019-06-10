(ns lore.util
  (:require [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [cognitect.anomalies :as anomalies])
  (:import [java.nio ByteBuffer]
           [java.util Base64]
           [java.io InputStream ByteArrayOutputStream FileNotFoundException]))

(defn input-stream?
  [v]
  (instance? InputStream v))

(defn byte-buf?
  [v]
  (instance? ByteBuffer v))

(defn byte-buf->bytes
  "Return all readable bytes from the given byte buffer as a byte array."
  [^ByteBuffer b]
  (let [bs (byte-array (.remaining b))]
    (.get b bs)
    bs))

(defn ->bytes
  [v]
  (cond
    (input-stream? v) (let [b (ByteArrayOutputStream.)]
                        (io/copy v b)
                        (.close v)
                        (.toByteArray b))
    (byte-buf? v)     (byte-buf->bytes v)
    (string? v)       (.decode (Base64/getDecoder) ^String v)
    (bytes? v)        v
    :else             (throw (IllegalArgumentException. (str "can't coerce " (type v) " to bytes")))))

(defn error?
  [v]
  (not (s/invalid? (s/conform ::anomalies/anomaly v))))

(defn dynaload
  [gaid fsym]
  (try
    (require (symbol (namespace fsym)))
    (catch FileNotFoundException fnf
      (throw (IllegalArgumentException.
                (str "Unable to load client, make sure " gaid " is on your classpath")
                fnf)))))

(defn dynarun
  [gaid fsym arg-map]
  (if-let [s (resolve fsym)]
    (s arg-map)
    (throw (IllegalArgumentException.
             (str "Unable to resolve entry point, make sure you have the correct version of " gaid " on your classpath")))))

(defn dynacall
  [gaid fsym arg-map]
  (dynaload gaid fsym)
  (dynarun gaid fsym arg-map))