(in-ns '[lore.repl])

; Example usage with softhsm and Java's PKCS11 provider.

; Quickstart for macOS, with homebrew:
; brew install softhsm
; softhsm2-util --init-token --slot 0 --label 'Your SoftHSM'

(import '[sun.security.pkcs11 SunPKCS11])
(import '[java.io ByteArrayInputStream])

(def config "name = SoftHSM\nlibrary = /usr/local/lib/softhsm/libsofthsm2.so\nslotListIndex = 0\nattributes(generate, *, *) = {\n   CKA_TOKEN = true\n}\nattributes(generate, CKO_CERTIFICATE, *) = {\n   CKA_PRIVATE = false\n}\nattributes(generate, CKO_PUBLIC_KEY, *) = {\n   CKA_PRIVATE = false\n}")

(def provider (SunPKCS11. (ByteArrayInputStream. (.getBytes config))))
(java.security.Security/addProvider provider)

(require '[lore.api.async.impl.jce :as jce] :reload)
(def store-password "...")
(def secret-store (jce/->secret-store {:store-type "PKCS11" :store-password store-password :padding :pkcs5}))

(:keystore secret-store)

(def plaintext (.getBytes "The Moon Rises at Noon."))

; this will generate a new secret key with ID 0000.
; this key is unexportable, so you can't use it with a different cipher than supported
; by the SunPKCS11 provider.
(def ciphertext (jce/encrypt (:keystore secret-store) plaintext {:padding :pkcs5 :key-id "0000" :generate-key? true}))

(require '[lore.api.async :as lore])
(require '[clojure.core.async :as async])

(def plaintext2 (async/<!! (lore/decrypt secret-store ciphertext)))
(lore/error? secret-store plaintext2)
; => false

(import '[java.security MessageDigest])
(MessageDigest/isEqual plaintext (:plaintext plaintext2))
; => true