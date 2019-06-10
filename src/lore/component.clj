(ns lore.component
  (:require [com.stuartsierra.component :as component]
            lore.api.async
            lore.api.async.impl.dummy
            [lore.util :refer [dynacall]]))

(defrecord SecretStore [store-type arguments impl]
  component/Lifecycle
  (start [this]
    (assoc this
      :impl (case store-type
              :kms (dynacall 'com.cognitect.aws/api
                             'lore.api.async.impl.kms/->kms-store
                             arguments)
              :dummy (lore.api.async.impl.dummy/->DummySecretStore)
              nil (throw (IllegalArgumentException. "argument `store-type' is required"))
              (throw (IllegalArgumentException. (str "invalid store-type: " store-type))))))
  (stop [this] this)

  lore.api.async/IAsyncSecretStore
  (error? [this v] (lore.api.async/error? (:impl this) v))
  (decrypt [this v] (lore.api.async/decrypt (:impl this) v)))