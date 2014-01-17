(ns clog.data_reducer.cached)

(defn hit
  [blocks]
  (reduce
    (fn [acc b] (+ acc (:hits b)))
    0
    blocks))

(defn processtime
  [blocks]
  (:avg-processtime (reduce
                      (fn [acc b]
                        (let [combined-hits (+ (:hits acc) (:hits b))]
                          (if (> combined-hits 0)
                            (assoc acc
                                   :avg-processtime (/ (+ (* (:hits acc) (:avg-processtime acc))
                                                          (* (:hits b) (:processtime b)))
                                                       combined-hits)
                                   :hits combined-hits)
                            acc)))
                      {:avg-processtime 0 :hits 0}
                      blocks)))
