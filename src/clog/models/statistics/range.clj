(ns clog.models.statistics.range
  (:require [clog.config :as config]
            [clj-time.core :as jtime]
            [clj-time.coerce :as coerce]
            [clog.caching :as caching]
            [clog.data_reducer.raw :as rawreducer]
            [clog.data_reducer.cached :as cachedreducer]
            )
  (:use [clog.database]))

(defn find-global-start-and-end []
  (let [f (get-first-logfileentry config/db)
        l (get-last-logfileentry config/db)]
    (if (or (nil? f) (nil? l))
      [(coerce/to-long (jtime/now)) (coerce/to-long (jtime/now))]
      [(:datetime f) (:datetime l)])))

(defn calculate-date-range-data
  [partition-reducer start end target-step-count records]
  (let [step-start start
        step-end end
        step-range (long (- step-end step-start))
        target-step-size (long (/ step-range target-step-count))]
    (->> records
         (partition-by (fn [r] (long (/ (- (:datetime r) step-start) target-step-size))))
         (reduce (fn [acc part]
                   (conj acc {:value (partition-reducer part)
                              :date (:datetime (first part))}))
                 [])
         ((fn [recs] {:start step-start :end step-end :items recs})))))

(defn calculate-cached-range-data
  [block-reducer start end block-lists]
  {:start start
   :end end
   :items (map
            (fn [step]
              {:date (:start step)
               :value (block-reducer (:blocks step)) })
            block-lists)})

(defn align-to-cache-resolution
  [value]
  (- value (mod value (first config/cache-resolutions))))

(defn calculate-cache-block-step-list
  [start end step-count]
  (let [real-start (align-to-cache-resolution start)
        real-end (align-to-cache-resolution end)
        step-size (long (Math/ceil (/ (- end start) step-count)))
        step-list (range step-count)]
    (map
      (fn [s]
        (let [step-start (align-to-cache-resolution (+ real-start
                                                       (* step-size s)))
              step-end (align-to-cache-resolution (+ step-start step-size))]
          [step-start (align-to-cache-resolution (min real-end step-end))]))
      step-list)))

(defn get-stepped-block-lists-for-range
  [start end step-count]
  (let [steps (calculate-cache-block-step-list start end step-count) ]
    (map (fn [[start end]]
           {:start start
            :blocks (caching/fetch-cache-blocks-for-range start end)})
         steps)))

(defn analyse
  ([] (apply analyse (find-global-start-and-end)))
  ([start end] (analyse start end 1))
  ([start end steps]
   (let [end (dec end)
         bounds {:datetime {:gteq start :lt end}}
         records (delay (get-logfileentries config/db bounds))
         cached-blocks (delay (get-stepped-block-lists-for-range start end steps))]
     {:start (delay start)
      :end (delay end)
      :steps (delay steps)
      :hits (delay (count-logfileentries config/db bounds))
      :hit-data (delay (calculate-cached-range-data cachedreducer/hit start end @cached-blocks))
      :processtime-data (delay (calculate-cached-range-data cachedreducer/processtime start end @cached-blocks))})))

