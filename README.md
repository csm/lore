# lore

Secret storage API.

## Usage

Using KMS:

```clojure
(require '[com.stuartsierra.component :refer [start]])
(require '[lore.component :refer :all])


(def lore (start (map->SecretStore {:store-type :kms})))

(require '[clojure.core.async :as async])
(require '[lore.api.async :refer :all])

(def plaintext (async/<!! (decrypt lore secret)))
```

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
