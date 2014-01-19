(ns clog.handler
  (:use compojure.core)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [clog.views.layout :as layout]
            [ring.adapter.jetty :as jetty]
            [clog.controller.index :as index]
            [clog.controller.statistics :as statistics]
            [clog.controller.logfile :as logfile]
            [clog.services.indexer :as indexer]
            [clog.config :as config]))



(defn app []
  (let [routes (defroutes app-routes
                 index/routes
                 statistics/routes
                 logfile/routes
                 (route/resources "/")
                 (route/not-found (layout/four-oh-four)))]
     (handler/site routes)))

(defn start []
  {:indexer (indexer/create)
   :jetty (jetty/run-jetty (app) {:port config/listen-port :join? false})})

(defn stop [s]
  (.stop (:jetty s))
  (indexer/stop (:indexer s)))

(defn restart [s]
  (stop s)
  (start))

(defn -main [& args]
  (start))
