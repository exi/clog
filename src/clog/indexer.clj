(ns clog.indexer
  (:require [clj-time.core :as jtime]
            [clj-time.coerce :as coerce]
            [clog.config :as config]
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

(defn regenerate-cache-block
  [block-spec]
  (let [start (:start block-spec)
        end (+ (:start block-spec) (:range block-spec) 1)
        records (get-logfileentries config/db {:datetime {:gteq start
                                                          :lt end}})
        old-block (get-cache-block config/db {:start (:start block-spec) :range (:range block-spec)})]
    (let [new-block {:start start 
                     :range (:range block-spec)
                     :hits (:value (first (:items (range-model/calculate-date-range-data
                                                    range-model/reduce-hit-data
                                                    start
                                                    end
                                                    1
                                                    records))))
                     :processtime (:value (first (:items (range-model/calculate-date-range-data
                                                           range-model/reduce-processtime-data
                                                           start
                                                           end
                                                           1
                                                           records))))}]
      (if old-block
        (update-cache-block! config/db (assoc new-block :id (:id old-block)))
        (save-cache-block! config/db new-block)))))

(defn index-changes
  [handle]
  (let [last-index (:last-index @handle)
        changed-datetimes (get-logfileenty-datetimes-inserted-after config/db (if last-index last-index 0))
        blocks-to-update (generate-block-update-list changed-datetimes)]
    (dorun (pmap regenerate-cache-block blocks-to-update))))

(defn start []
  (let [start-time (coerce/to-long (jtime/now))
        handle (atom {:stop false
                      :last-index (get-last-index-run-datetime config/db)})]
    (println "start indexing")
    (index-changes handle)
    (println "indexing done")
    (save-index-run-at! config/db start-time)
    (swap! handle #(assoc % :last-index start-time))
    (println "index run saved")
    handle))

(defn stop [h]
  (swap! h #(assoc % :stop true)))
