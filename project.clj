(defproject clog "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [compojure "1.1.6"]
                 [ring/ring-jetty-adapter "1.2.0"]
                 [ring/ring-core "1.2.0"]
                 [clojureql "1.0.4"]
                 [postgresql/postgresql "8.4-702.jdbc4"]
                 [clj-time "0.6.0"]
                 [hiccup "1.0.4"]]
  :plugins [
            [lein-ring "0.8.8"]
            [lein-swank "1.4.5"]
            [lein-lesscss "1.2"]
            ]
  :ring {:handler clog.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-serve "0.1.2"]
                        [ring-mock "0.1.5"]]}})
