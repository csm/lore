(ns lore.api.async)

(defprotocol IAsyncSecretStore
  (error? [this result]
    "Tell if result is an error. Errors are anomalies as defined in
    the cognitect.anomalies library.")

  (decrypt [this ciphertext]
    "Decrypt ciphertext. Returns a promise channel that will yield a
    map that contains key :plaintext with the decrypted plaintext,
    or an error map.

    Ciphertext may be a base-64 encoded string, byte array, or a
    java.nio.ByteBuffer. The returned plaintext will be a
    byte array."))