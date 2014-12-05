(defproject storage "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [commons-io/commons-io "2.4"]
                 [ring/ring-core "1.3.0"]
                 [ring/ring-json "0.3.1"]
                 [ring/ring-defaults "0.1.2"]
                 [http-kit "2.1.18"]
                 [compojure "1.2.0"]
                 [javax.servlet/servlet-api "2.5"]
                 [clojurewerkz/spyglass "1.1.0"]
                 [environ "1.0.0"]
                 [clj-aws-s3 "0.3.10" :exclusions [joda-time]]
                 [org.clojure/tools.nrepl "0.2.3"]
                 [org.clojure/tools.namespace "0.2.4"]]
  :plugins [[lein-environ "1.0.0"]]
  :app-class storage.core/app
  :main ^:skip-aot storage.core
  :target-path "tmp/"
  :profiles {:uberjar {:aot :all}})
