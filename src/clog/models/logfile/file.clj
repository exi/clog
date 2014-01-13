(ns clog.models.logfile.file
  (:require [clojure.java.io :as io]))

(defn get-file
  [path]
  (try
    (let [f (io/file path)]
      (when (and (.isFile f) (.canRead f))
        f))
    (catch Exception e nil)))

(defn get-line-seq
  [file]
  (line-seq (io/reader file))) 

(defn open
  [path]
  (if-let [file (get-file path)]
    {:path path
     :lines (get-line-seq file)
     :length (.length file)}
    nil))
