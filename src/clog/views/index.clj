(ns clog.views.index
  (:use [hiccup.core :only [html]])
  (:require
    [clog.views.layout :as layout]
    [clog.views.logfiles :as logfiles]))

(defn view
  [lfiles]
  (layout/common
    "CLog logfile analyzer"
    (logfiles/view lfiles)))
