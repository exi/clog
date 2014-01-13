(ns clog.models.logentry
  (:require [clog.config :as config]
            [clj-time.core :as jtime]
            [clj-time.coerce :as coerce])
  (:use [clog.database]))

(defn insert! [entries] (save-logfileentries!
                          config/db
                          (map #(assoc % :inserted (coerce/to-long (jtime/now))) entries)))
