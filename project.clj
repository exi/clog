(defproject clog "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.6.0-alpha2"]
                 [compojure "1.1.6"]
                 [ring/ring-jetty-adapter "1.2.0"]
                 [ring/ring-core "1.2.0"]
                 [clj-time "0.6.0"]
                 [org.clojure/java.jdbc "0.3.0-beta2"]
                 [mysql/mysql-connector-java "5.1.25"]
                 [named-re "1.0.0"]
                 [org.clojure/data.json "0.2.3"]
                 [hiccup "1.0.4"]
                 [com.mchange/c3p0 "0.9.2.1"]]
  :plugins [
            [lein-ring "0.8.8"]
            [lein-swank "1.4.5"]
            [lein-lesscss "1.2"]
            ]
  :ring {:handler clog.handler/app}
  :main clog.handler
  :jvm-opts ["-Xmx10g" "-server"]
  :profiles {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-serve "0.1.2"]
                        [ring-mock "0.1.5"]]}})
