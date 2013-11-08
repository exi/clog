(ns clog.models.logfiles
  (:require [clog.config :as config]
            [clojureql.core :as cql]
            [clog.models.logfile :as logfile]
            ))

(defn all
  []
  (map
    logfile/data->entity
    @(cql/table config/db :logfiles)))
