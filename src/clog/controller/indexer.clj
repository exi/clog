(ns clog.controller.indexer
  (:use [compojure.core :only [defroutes GET POST]])
  (:require [ring.util.response :as r]
            [clog.views.layout :as layout]
            [clog.indexer :as indexer]))

(defn index! []
  (try
    (indexer/start)
    (r/redirect "/")))

(defroutes routes
  (GET ["/indexer/run"] [] (index!)))
