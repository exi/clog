(ns clog.controller.statistics
  (:use [compojure.core :only [defroutes GET POST]])
  (:require [clog.models.statistics.range :as range-model]
            [clj-time.core :as jtime]
            [clj-time.coerce :as coerce]
            [clog.views.statistics.overview :as overview-view]
            [clog.views.layout :as layout]))

(defn overview
  []
  (let [now (jtime/now)]
    (overview-view/render
      [{:title "Current Month"
        :model (let [start (coerce/to-long (jtime/date-time (jtime/year now) (jtime/month now)))
                     end (coerce/to-long (jtime/plus (jtime/date-time (jtime/year now) (jtime/month now)) (jtime/months 1)))]
                 (range-model/analyse start end))}
       {:title "Previous Month"
        :model (let [this-month (jtime/date-time (jtime/year now) (jtime/month now))
                     start (coerce/to-long (jtime/minus this-month (jtime/months 1)))
                     end (coerce/to-long this-month)]
                 (range-model/analyse start end))}
       {:title "All Time"
        :model (let [r (range-model/find-global-start-and-end)]
                 (range-model/analyse (first r) (second r)))}])))

(defn generate-overview-data
  [t start end steps]
  (let [viewfun (case t
                  :hit overview-view/render-hit-data
                  :processtime overview-view/render-processtime-data)]
    (viewfun (range-model/analyse start end steps))))

(defroutes routes
  (GET ["/statistics/overview"] [] (overview))
  (GET ["/statistics/range-hit-data/:start/:end/:steps" :start #"[0-9]+" :end #"[0-9]+" :steps #"[0-9]+"]
       [start end steps] (generate-overview-data :hit (read-string start) (read-string end) (read-string steps)))
  (GET ["/statistics/range-processtime-data/:start/:end/:steps" :start #"[0-9]+" :end #"[0-9]+" :steps #"[0-9]+"]
       [start end steps] (generate-overview-data :processtime (read-string start) (read-string end) (read-string steps))))
