(ns clog.controller.logfile
  (:use [compojure.core :only [defroutes GET POST]])
  (:require [clojure.string :as str]
            [clog.models.logfiles :as logfiles]
            [clog.views.layout :as layout]
            ))

(defn scan
  [id]
  (layout/common
    "Scan"
    (layout/container "scan" (str "scanned " id))))

(defroutes routes
  (GET "/logfile/scan/:id" [id] (scan id)))
