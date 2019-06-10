(in-ns 'lore.repl)

"This is an example script for testing KMS against an actual AWS account."

(require 'lore.component)
(require '[com.stuartsierra.component :refer [start]])

(def lore (start (lore.component/map->SecretStore {:store-type :kms})))

(require '[cognitect.aws.client.api :as aws])
(def kms-client (-> lore :impl :kms-client))

; create a new key for testing; you should only do this once, and then
; use that key ID if you run more tests.
(def create-key (aws/invoke kms-client {:op :CreateKey :request {}}))

; create a new secret
(def secret "This is my secret; there are many others like it, but this one is mine.")
(def encrypt (aws/invoke kms-client {:op :Encrypt
                                     :request {:Plaintext (.getBytes secret)
                                               :KeyId (-> create-key :KeyMetadata :KeyId)}}))

(require '[lore.util :refer [->bytes]])
(def ciphertext-blob (->bytes (:CiphertextBlob encrypt)))

(require '[clojure.core.async :as async])
(require '[lore.api.async :as lore])
(def result (async/<!! (lore/decrypt lore ciphertext-blob)))
(def plaintext (String. (:plaintext result)))
(= secret plaintext)