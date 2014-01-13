(ns clog.caching
  (:require [clog.config :as config])
  (:use [clog.database]))

(defn find-best-cache-block
  [start end]
  (let [resolutions (reverse config/cache-resolutions)
        best-res (first
                   (filter
                     #(and (= (mod start %) 0)
                           (<= (+ start (dec %)) end))
                     resolutions))]
    {:start start
     :range best-res}))

(defn get-cache-block-list-for-range
  [start end]
  (loop [position start blocks []]
    (if (>= position end)
      blocks
      (let [block (find-best-cache-block position end)]
        (recur (+ position (:range block)) (conj blocks block))))))

(defn fetch-cache-blocks-for-range
  [start end]
  (get-cache-blocks config/db {:$or (get-cache-block-list-for-range start end)}))
