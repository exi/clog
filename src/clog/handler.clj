(ns clog.handler
  (:use compojure.core)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [clog.views.layout :as layout]
            [ring.adapter.jetty :as jetty]
            [clog.controller.index :as index]
            [clog.controller.statistics :as statistics]
            [clog.controller.logfile :as logfile]
            [clog.controller.indexer :as indexer]))

(defroutes app-routes
  index/routes
  statistics/routes
  logfile/routes
  indexer/routes
  (route/resources "/")
  (route/not-found (layout/four-oh-four)))

(def app
   (handler/site app-routes))

(defn start []
  (jetty/run-jetty app {:port 3000 :join? false}))

(defn stop [s]
  (.stop s))

(defn restart [s]
  (stop s)
  (start))

(defn -main [& args]
  (start))
