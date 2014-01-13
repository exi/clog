(ns clog.models.statistics.range
  (:require [clog.config :as config]
            [clj-time.core :as jtime]
            [clj-time.coerce :as coerce]
            [clog.caching :as caching]
            )
  (:use [clog.database]))

(defn find-global-start-and-end
  []
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

(defn reduce-hit-data
  [part]
  (count part))

(defn reduce-processtime-data
  ([part]
   (let [c (count part)]
     (if (pos? c)
       (/
        (reduce
          (fn [acc item]
            (+ acc (if-let [pt (:processtime item)]
                     pt
                     0)))
          0
          part)
        c)
       0))))

(defn calculate-cached-range-data
  [block-reducer start end block-lists]
  {:start start
   :end end
   :items (map
            (fn [step]
              {:date (:start step)
               :value (block-reducer (:blocks step)) })
            block-lists)})

(defn hits-block-reducer
  [blocks]
  (reduce
    (fn [acc b] (+ acc (:hits b)))
    0
    blocks))

(defn processtime-block-reducer
  [blocks]
  (reduce
    (fn [acc b] (+ acc (:hits b)))
    0
    blocks))

(defn get-stepped-block-lists-for-range
  [start end step-count]
  (let [step-size (long (Math/ceil (/ (- end start) step-count)))
        step-list (range step-count)
        steps (map
                (fn [s]
                  (let [step-start (+ start (* step-size s))
                        step-end (+ step-start step-size) ]
                    [step-start (min end step-end)]))
                step-list)]
    (map (fn [[start end]]
           {:start start
            :blocks (caching/fetch-cache-blocks-for-range start end)}) steps)
    ))

(defn analyse
  ([] (apply analyse (find-global-start-and-end)))
  ([start end] (analyse start end 800))
  ([start end steps]
   (let [end (dec end)
         bounds {:datetime {:gteq start :lt end}}
         records (delay (get-logfileentries config/db bounds))
         cached-blocks (delay (get-stepped-block-lists-for-range start end steps))
         ]
     {:start (delay start)
      :end (delay end)
      :steps (delay steps)
      :hits (delay (count-logfileentries config/db bounds))
      :hit-data (delay (calculate-cached-range-data hits-block-reducer start end @cached-blocks))
      :processtime-data (delay (calculate-date-range-data reduce-processtime-data start end steps @records))})))

