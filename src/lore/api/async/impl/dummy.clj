(ns lore.api.async.impl.dummy
  "Dummy secret store implementation, which does not decrypt."
  (:require [clojure.core.async :as async]
            lore.api.async
            [lore.util :refer :all]))

(defrecord DummySecretStore []
  lore.api.async/IAsyncSecretStore
  (error? [_this v] (error? v))
  (decrypt [_this ciphertext]
    (doto (async/promise-chan)
      (async/put! (try (->bytes ciphertext)
                       (catch Exception e
                         {:cognitect.anomalies/category :cognitect.anomalies/fault
                          :cognitect.anomalies/message (.getMessage e)
                          :exception e}))))))