(ns clog.services.indexer
  (:require [clj-time.core :as jtime]
            [clj-time.coerce :as coerce]
            [clog.config :as config]
            [clog.data_reducer.raw :as rawreducer]
            [clog.data_reducer.cached :as cachedreducer]
            [clog.models.statistics.range :as range-model])
  (:use clog.database))

(defn get-dirty-block-by-resolution
  [datetime resolution]
  (let [aligned-edge (- datetime (mod datetime resolution))]
    {:start aligned-edge
     :range resolution}))

(defn generate-block-update-lazy-list
  [dirty-datetimes resolutions]
  (if (empty? resolutions)
    nil
    (lazy-seq
      (concat
        (pmap #(get-dirty-block-by-resolution % (first resolutions)) dirty-datetimes)
        (generate-block-update-lazy-list dirty-datetimes (rest resolutions))))))

(defn generate-block-update-list
  [changed-datetimes]
  (distinct (generate-block-update-lazy-list changed-datetimes config/cache-resolutions)))

(defn get-lower-range
  [r]
  (let [i (.indexOf config/cache-resolutions r)]
    (if (>= i 1)
      (get config/cache-resolutions (dec i))
      (first config/cache-resolutions))))

(defn regenerate-cache-block-from-raw-data
  [block-spec]
  (let [start (:start block-spec)
        end (+ (:start block-spec) (:range block-spec) 1)
        records (get-logfileentries config/db {:datetime {:gteq start
                                                          :lt end}})]
    {:start start 
     :range (:range block-spec)
     :hits (rawreducer/hit records)
     :processtime (rawreducer/processtime records)}))

(defn regenerate-cache-block-from-cached-data
  [block-spec]
  (let [start (:start block-spec)
        end (+ (:start block-spec) (:range block-spec) 1)
        blocks (get-cache-blocks config/db {:start {:gteq start
                                                    :lt end}
                                            :range (get-lower-range (:range block-spec))})]
    {:start start
     :range (:range block-spec)
     :hits (cachedreducer/hit blocks)
     :processtime (cachedreducer/processtime blocks)}))

(defn regenerate-cache-block
  [block-spec]
  (let [old-block (first (get-cache-blocks config/db {:start (:start block-spec) :range (:range block-spec)}))
        new-block (if (= (:range block-spec) (first config/cache-resolutions))
                    (regenerate-cache-block-from-raw-data block-spec)
                    (regenerate-cache-block-from-cached-data block-spec))]
    (if old-block
      (update-cache-block! config/db (assoc new-block :id (:id old-block)))
      (save-cache-block! config/db new-block))))

(defn index-changes
  [handle]
  (let [last-index (:last-index @handle)
        changed-datetimes (get-logfileentry-datetimes-inserted-after config/db (if last-index last-index 0))
        blocks-to-update (generate-block-update-list changed-datetimes)]
    (dorun (pmap regenerate-cache-block blocks-to-update))
    (if (seq blocks-to-update) true false)))

(defn run [handle]
  (let [start-time (coerce/to-long (jtime/now))]
    (when (index-changes handle)
      (save-index-run-at! config/db start-time)
      (swap! handle #(assoc % :last-index start-time)))))

(defn setup []
  (let [handle (atom {:stop false
                      :last-index (get-last-index-run-datetime config/db)})]
    (future
      (while (false? (:stop @handle))
        (try
          (run handle)
          (catch Exception e (println (str "Indexer caught: " (.getMessage e)))))
        (Thread/sleep 10000)))
    (println "indexer setup done, last index at" (:last-index @handle))
    handle))

(defprotocol IndexService
  (stop [this]))

(defrecord Indexer [handle]
  IndexService
  (stop [this] 
    (println "indexer stopping")
    (swap! handle #(assoc % :stop true))))

(defn create []
  (Indexer. (setup)))
