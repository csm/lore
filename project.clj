(defproject com.github.csm/lore "0.1.3-SNAPSHOT"
  :description "Secret storage API for clojure"
  :url "https://github.com/csm/lore"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [com.cognitect/anomalies "0.1.12"]
                 [com.stuartsierra/component "0.3.2"]]
  :profiles {:provided {:dependencies [[com.cognitect.aws/api "0.8.305"]
                                       [com.cognitect.aws/endpoints "1.1.11.565"]
                                       [com.cognitect.aws/kms "718.2.448.0"]
                                       [buddy/buddy-core "1.5.0"]
                                       [com.cognitect/transit-clj "0.8.313"]]}
             :repl {:dependencies [[com.cognitect.aws/api "0.8.305"]
                                   [com.cognitect.aws/endpoints "1.1.11.565"]
                                   [com.cognitect.aws/kms "718.2.448.0"]
                                   [buddy/buddy-core "1.5.0"]
                                   [com.cognitect/transit-clj "0.8.313"]]
                    :source-paths ["scripts"]}}
  :repl-options {:init-ns lore.repl}
  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version"
                   "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag"]
                  ["deploy" "clojars"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]])
