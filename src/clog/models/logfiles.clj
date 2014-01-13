(ns clog.models.logfiles
  (:require [clog.config :as config]
            [clog.models.logfile :as logfile]
            [clojure.java.io :as io])
  (:use clog.database))

(defn all
  []
  (sort (fn [a b] (< (:id a) (:id b))) (get-logfiles config/db {})))

(defn import-from-dir
  [dirname]
  (doseq [f (filter #(.isFile %) (file-seq (io/file dirname)))]
    (save-logfile! config/db {:path (str f) :lines 0 :length 0 :last_scan nil})
    (println "added " (str f))))

(defn scan-all
  []
  (doseq [lf (get-logfiles config/db {})] (logfile/scan! (:id lf)) (println "scanned " (:path lf))))
