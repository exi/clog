(ns clog.models.logfile.scanner
  (:require [clog.models.logfile.parser :as parser]
            [clojure.java.io :as io]))

(defn parse-lines
  [lines line-format]
  (let [line-format (if line-format
                      line-format
                      (first (remove nil? (map parser/detect-format lines))))]
    (println "detected " line-format)
    (remove nil? (pmap #(parser/parse-line % line-format) lines)) ))

(defn parse-file
  [file skip line-format]
  (parse-lines (drop skip (:lines file)) line-format))
