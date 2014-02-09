(ns clog.database.mongodb
  (:require [clj-time.core :as jtime]
            [clojure.string :as string]
            [monger.core :as mg]
            [monger.collection :as mc]
            )
  (:import [com.mongodb MongoOptions ServerAddress]
           [org.bson.types ObjectId])
  (:refer-clojure :exclude [sort find])
  (:use clog.database
        monger.query))

(def bulk-insert-size 100)

(def logfile-collection "logfiles")
(def logfileentries-collection "logfileentries")
(def index_runs-collection "index_runs")
(def time_cache-collection "time_cache")

(defn add-id [item] (assoc item :_id (ObjectId.)))
(defn id->_id [item]
  (if (contains? item :id)
    (dissoc (assoc item :_id (ObjectId. (:id item))) :id)
    item))
(defn _id->id [item]
  (if (contains? item :_id)
    (dissoc (assoc item :id (str (:_id item))) :_id)
    item))

(defn rename-operators [omap]
  (reduce
    (fn [acc [k v]]
      (let [newk (case k
                   :gteq "$gte"
                   :lt "$lt"
                   k)]
        (assoc acc newk v)))
    {}
    omap))

(defn where->query [where]
  (reduce
    (fn [acc [k v]]
       (if (map? v)
        (assoc acc k (rename-operators v))
        (if (= k :$or)
          (assoc acc "$or" (map where->query v))
          (assoc acc k v))))
    {}
    (id->_id where)))

(defrecord MongoDB []
  Database
  (get-logfiles [this where] (map _id->id (mc/find-maps logfile-collection (where->query where))))
  (get-logfileentries [this where] (map _id->id (mc/find-maps logfileentries-collection (where->query where))))
  (get-first-logfileentry [this]
    (first (map _id->id (with-collection logfileentries-collection
                          (find {})
                          (fields [:datetime])
                          (sort (array-map :datetime 1))
                          (limit 1)))))
  (get-last-logfileentry [this]
    (first (map _id->id (with-collection logfileentries-collection
                          (find {})
                          (fields [:datetime])
                          (sort (array-map :datetime -1))
                          (limit 1)))))
  (get-logfileentry-datetimes-inserted-after [this start]
    (remove nil?
            (map :datetime
                 (with-collection logfileentries-collection
                   (find (where->query {:inserted {:gteq start}}))
                   (fields [:datetime])
                   (sort (array-map :datetime 1))))))
  (get-last-index-run-datetime [this]
    (first (map :timestamp (with-collection index_runs-collection
                          (find {})
                          (fields [:timestamp])
                          (sort (array-map :timestamp -1))
                          (limit 1)))))
  (get-cache-blocks [this where] (map _id->id (with-collection time_cache-collection (find (where->query where)))))
  (count-logfileentries [this where] (mc/count logfileentries-collection (where->query where)))
  (save-logfile! [this logfile] (mc/insert logfile-collection (add-id logfile)))
  (save-logfileentries! [this entries]
    (loop [e entries]
      (when (seq e)
        (do (mc/insert-batch
              logfileentries-collection
              (map add-id (take bulk-insert-size e)))
            (recur (drop bulk-insert-size e))))))
  (save-index-run-at! [this datetime] (mc/insert index_runs-collection (add-id {:timestamp datetime})))
  (save-cache-block! [this block] (mc/insert time_cache-collection (add-id block)))
  (update-logfile! [this logfile] (mc/update-by-id logfile-collection (:_id (id->_id logfile)) (id->_id logfile)))
  (update-cache-block! [this block] (mc/update-by-id time_cache-collection (:_id (id->_id block)) (id->_id block)))
  (delete-logfiles! [this where] (mc/remove logfile-collection (where->query where)))
  (delete-logfileentries! [this where] (mc/remove logfileentries-collection (where->query where))))

(defn create
  ([] (create {}))
  ([config]
   (if (or (contains? config :host) (contains? config :port))
     (mg/connect! config)
     (mg/connect!))
   (mg/set-db! (mg/get-db (if (:dbname config) (:dbname config) "clog")))
   (MongoDB.)))
