(ns clog.models.logfiles
  (:require [clj-time.core :as jtime]
            [clj-time.coerce :as coerce]
            [clog.config :as config]
            [clog.models.logfile :as logfile]
            [clojure.java.io :as io])
  (:use clog.database))

(defn all
  []
  (get-logfiles config/db {}))

(defn import-from-dir
  [dirname]
  (doseq [f (filter #(.isFile %) (file-seq (io/file dirname)))]
    (save-logfile! config/db {:path (str f) :lines 0 :length 0 :last_scan nil})
    (println "added " (str f))))

(defn scan-all
  []
  (let [starttime (coerce/to-long (jtime/now))]
    (doseq [lf (get-logfiles config/db {})] (logfile/scan! (:id lf)) (println "scanned " (:path lf)))
    (println
      "scan took"
      (long (/ (- (coerce/to-long (jtime/now)) starttime) 1000))
      "seconds")))
