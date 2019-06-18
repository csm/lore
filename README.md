# lore

[![Clojars Project](https://img.shields.io/clojars/v/com.github.csm/lore.svg)](https://clojars.org/com.github.csm/lore)

Secret storage API.

## Usage

`[com.github.csm/lore "0.1.2"]`

Provided backends include [KMS](https://aws.amazon.com/kms/), [buddy](https://github.com/funcool/buddy),
and JCE (which uses `java.security.KeyStore`, `javax.crypto.Cipher`, and friends). Also available
is a "dummy" backend that does no decryption.

### KMS

```clojure
(require '[com.stuartsierra.component :refer [start]])
(require '[lore.component :refer :all])


(def lore (start (map->SecretStore {:store-type :kms})))

(require '[clojure.core.async :as async])
(require '[lore.api.async :refer :all])

(def plaintext (async/<!! (decrypt lore secret)))
```

KMS requires [Cognitect's AWS library](https://github.com/cognitect-labs/aws-api).
Your project dependencies should include (update versions as appropriate):

```clojure
[[com.cognitect.aws/api "0.8.305"]
 [com.cognitect.aws/endpoints "1.1.11.565"]
 [com.cognitect.aws/kms "718.2.448.0"]]
```

### JCE

The JCE backend requires [transit](https://github.com/cognitect/transit-clj). You can
refine what format to use for transit; the default is `:msgpack`.

The JCE backend is most interesting with a PKCS#11 compatible Hardware Security Module.
This can be run against [SoftHSM](https://www.opendnssec.org/softhsm/) for testing
without a real HSM.

## License

Copyright © 2019 Noon Home

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
