(in-ns '[lore.repl])

; Example usage with softhsm and Java's PKCS11 provider.

; Quickstart for macOS, with homebrew:
; brew install softhsm
; softhsm2-util --init-token --slot 0 --label 'Your SoftHSM'
; enter your SoftHSM PINs (or supply command-line arguments)

(import '[sun.security.pkcs11 SunPKCS11])
(import '[java.io ByteArrayInputStream])

(def config "name = SoftHSM
library = /usr/local/lib/softhsm/libsofthsm2.so
slotListIndex = 0
attributes(generate, *, *) = {
   CKA_TOKEN = true
}
attributes(generate, CKO_CERTIFICATE, *) = {
   CKA_PRIVATE = false
}
attributes(generate, CKO_PUBLIC_KEY, *) = {
   CKA_PRIVATE = false
}")

(def provider (SunPKCS11. (ByteArrayInputStream. (.getBytes config))))
(java.security.Security/addProvider provider)

(require '[lore.api.async.impl.jce :as jce] :reload)
(def store-password "...") ; set to your user PIN from softhsm2-util --init-token.
(def secret-store (jce/->secret-store {:store-type "PKCS11" :store-password store-password :padding :pkcs5}))

(:keystore secret-store)

(def plaintext (.getBytes "The Moon Rises at Noon."))

; this will generate a new secret key with ID 0000.
; this key is unexportable, so you can't use it with a different cipher than supported
; by the SunPKCS11 provider.
; We give :pkcs5 padding here
(def ciphertext (jce/encrypt (:keystore secret-store) plaintext {:padding :pkcs5 :key-id "0000" :generate-key? true}))

; you can create your own padding schemes by defining new pad and unpad multimethods
; pad arguments:
;    format -- a keyword format
;    block-size -- the cipher block size
;    input-size -- the input size
; pad should return a byte array containing the padding bytes
; unpad arguments:
;    format -- a keyword format
;    input -- the input to unpad
; unpad should return the input, with the padding bytes removed

(require '[lore.api.async :as lore])
(require '[clojure.core.async :as async])

(def plaintext2 (async/<!! (lore/decrypt secret-store ciphertext)))
(lore/error? secret-store plaintext2)
; => false

(import '[java.security MessageDigest])
(MessageDigest/isEqual plaintext (:plaintext plaintext2))
; => true