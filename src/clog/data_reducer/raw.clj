(ns clog.data_reducer.raw)

(defn hit [rows]
  (count rows) )

(defn processtime
  [rows]
  (let [c (count rows)]
    (if (pos? c)
      (/
       (reduce
         (fn [acc item]
           (+ acc (if-let [pt (:processtime item)]
                    pt
                    0)))
         0
         rows)
       c)
      0)))
