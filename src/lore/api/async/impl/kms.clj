(ns lore.api.async.impl.kms
  "Secret store implementation on top of AWS KMS."
  (:require [clojure.core.async :as async]
            [clojure.spec.alpha :as s]
            [cognitect.anomalies :as anomalies]
            [cognitect.aws.client.api :refer [client]]
            [cognitect.aws.client.api.async :as aws]
            lore.api.async
            [lore.util :refer :all]))

(defrecord AsyncKMSSecretStore [kms-client]
  lore.api.async/IAsyncSecretStore
  (error? [_this result]
    (not (s/invalid? (s/conform ::anomalies/anomaly result))))

  (decrypt [this ciphertext]
    (async/go
      (try
        (let [request {:CiphertextBlob (->bytes ciphertext)}
              response (async/<! (aws/invoke kms-client {:op :Decrypt :request request}))]
          (if (lore.api.async/error? this response)
            response
            (with-meta {:plaintext (->bytes (:Plaintext response))}
                       {:aws.kms/key-id (:KeyId response)})))
        (catch Exception e
          {::anomalies/category ::anomalies/fault
           ::anomalies/message (.getMessage e)
           :exception e})))))

(defn ->kms-store
  [arg-map]
  (->AsyncKMSSecretStore (client (assoc arg-map :api :kms))))