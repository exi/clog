(ns clog.views.statistics.range
  (:require [clog.views.layout :as layout]
            [clj-time.coerce :as coerce]
            [clojure.data.json :as json]))

(defn step-chart-block
  [model url label]
  [:div {:class "render-step-chart"
         :data-step-chart (json/write-str {:base-url url
                                           :start (coerce/to-long @(:start model))
                                           :end (coerce/to-long @(:end model))
                                           :y-axis-label label})}])

(defn render
  [model]
  [:div
   [:p (str "Hits: " @(:hits model))]
   (step-chart-block model "/statistics/range-hit-data/" "Hits")
   (step-chart-block model "/statistics/range-processtime-data/" "Process time")])

(defn render-data
  [model k]
  (let [data @(get model k)]
    (json/write-str {:start (:start data)
                     :end (:end data)
                     :items (:items data)})))

(defn render-hit-data
  [model]
  (render-data model :hit-data))


(defn render-processtime-data
  [model]
  (render-data model :processtime-data))
