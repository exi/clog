(ns clog.controller.statistics
  (:use [compojure.core :only [defroutes GET POST]])
  (:require [clog.models.statistics.range :as range-model]
            [clj-time.core :as jtime]
            [clj-time.coerce :as coerce]
            [clog.views.statistics.range :as range-view]
            [clog.views.layout :as layout]))

(defn statistics
  []
  (layout/common
    "Statistics"
    (layout/container
      "Current Month"
      (let [now (jtime/now)
            start (coerce/to-long (jtime/date-time (jtime/year now) (jtime/month now)))
            end (coerce/to-long (jtime/plus (jtime/date-time (jtime/year now) (jtime/month now)) (jtime/months 1)))]
        (range-view/render (range-model/analyse start end))))
    (layout/container
      "Previous Month"
      (let [now (jtime/now)
            this-month (jtime/date-time (jtime/year now) (jtime/month now))
            start (coerce/to-long (jtime/minus this-month (jtime/months 1)))
            end (coerce/to-long this-month)]
        (range-view/render (range-model/analyse start end))))
    (layout/container
      "All Time"
      (let [r (range-model/find-global-start-and-end)]
        (range-view/render (range-model/analyse (first r) (second r)))))))

(defn generate-range-data
  [t start end steps]
  (let [viewfun (case t
                  :hit range-view/render-hit-data
                  :processtime range-view/render-processtime-data)]
    (viewfun (range-model/analyse start end steps))))

(defroutes routes
  (GET ["/statistics"] [] (statistics))
  (GET ["/statistics/range-hit-data/:start/:end/:steps" :start #"[0-9]+" :end #"[0-9]+" :steps #"[0-9]+"]
       [start end steps] (generate-range-data :hit (read-string start) (read-string end) (read-string steps)))
  (GET ["/statistics/range-processtime-data/:start/:end/:steps" :start #"[0-9]+" :end #"[0-9]+" :steps #"[0-9]+"]
       [start end steps] (generate-range-data :processtime (read-string start) (read-string end) (read-string steps))))
