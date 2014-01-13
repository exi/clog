(ns clog.models.logfile
  (:require [clog.config :as config]
            [clog.models.logfile.file :as file]
            [clog.models.logfile.scanner :as scanner]
            [clog.models.logentry :as logentry]
            [clj-time.core :as jtime]
            [clj-time.coerce :as coerce])
  (:use [clog.database]))

(defn calculate-skip
  [file entity]
  (if (< (:length file) (:length entity))
    0
    (:lines entity)))

(defn scan-and-save!
  [entity]
  (if-let [f (file/open (:path entity))]
    (let [skip (calculate-skip f entity)
          records (scanner/parse-file f skip (:format entity))]
      (logentry/insert! records)
      (println (count records) "records inserted")
      (let [entity (assoc entity
                          :lines (+ skip (count records))
                          :length (:length f)
                          :last_scan (coerce/to-long (jtime/now)))]
        (update-logfile! config/db entity)
        entity))
    (throw (Exception. "File not Found"))))

(defn scan!
  [id]
  (if-let [entity (first (get-logfiles config/db {:id id}))]
    (do
      (scan-and-save! entity)
      :success)
    (throw (Exception. (str "Entity " id " not found")))))

(defn delete!
  [id]
  (if-let [entity (get-logfiles config/db {:id id})]
    (do
      (delete-logfiles! config/db {:id id})
      :success)
    (throw (Exception. (str "Entity " id " not found")))))

(defn add!
  [path]
  (do
    (save-logfile! config/db {:path path :lines 0 :length 0 :last_scan nil})
    :success))
