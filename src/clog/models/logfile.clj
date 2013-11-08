(ns clog.models.logfile
  (:require [clog.config :as config]
            [clj-time.coerce :as coerce]
            [clojureql.core :as cql]))

(defn data->entity
  [data]
  (assoc-in
    data
    [:last_access]
    (if (:last_access data)
      (coerce/from-sql-date (:last_access data))
      nil)))

(defn get-entity-by-id
  [id]
  (data->entity (first @(cql/select (cql/table config/db :logfiles) (cql/where (= :id id))))))

(defn scan
  [id]
  (let [entity (get-entity-by-id id)]))
