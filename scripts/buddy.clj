(in-ns 'lore.repl)

; testing for buddy impl

(require '[com.stuartsierra.component :refer [start]])
(require 'lore.component :reload)
(require '[buddy.core.nonce :as nonce])

; can also pass :options in :arguments, to specify cipher alg.
(def lore (start (lore.component/map->SecretStore {:store-type :buddy
                                                   :arguments {:master-key (nonce/random-bytes 32)}})))

; encrypt a value
(def secret "This is my secret; there are many others like it, but this one is mine.")
(def ciphertext-blob (lore.api.async.impl.buddy/encrypt (-> lore :impl :master-key) (.getBytes secret) nil))

; decrypt
(require '[clojure.core.async :as async])
(require '[lore.api.async :as lore])
(def plaintext (async/<!! (lore/decrypt lore ciphertext-blob)))
(= secret (String. ^"[B" (:plaintext plaintext)))

; decrypt an invalid value
(require '[clojure.edn :as edn])
(import 'java.util.Base64)
(def invalid-iv (-> (String. ciphertext-blob)
                    (edn/read-string)
                    (assoc :iv (.encodeToString (Base64/getEncoder) (nonce/random-bytes 16)))
                    (pr-str)
                    (.getBytes)))
(def plaintext2 (async/<!! (lore/decrypt lore invalid-iv)))
(lore/error? lore plaintext2)

; decrypt an invalid ciphertext
(def invalid-ct (-> (String. ciphertext-blob)
                    (edn/read-string)
                    (update :ciphertext #(.decode (Base64/getDecoder) %))
                    (update :ciphertext #(do (aset-byte % 0 (.byteValue (inc (aget % 0))))
                                             %))
                    (update :ciphertext #(.encodeToString (Base64/getEncoder) %))
                    (pr-str)
                    (.getBytes)))
(def plaintext3 (async/<!! (lore/decrypt lore invalid-ct)))
(lore/error? lore plaintext3)