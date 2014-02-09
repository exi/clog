(ns clog.controller.logfile
  (:use [compojure.core :only [defroutes GET POST]])
  (:require [clog.models.logfile :as logfile]
            [clog.models.logfiles :as logfiles]
            [ring.util.response :as r]
            [clog.views.layout :as layout]))

(defn scan!
  [id]
  (try
    (logfile/scan! id)
    (r/redirect "/")
    ;(catch Exception e (layout/error "Scanning error" e))
    ))

(defn delete!
  [id]
  (try
    (logfile/delete! id)
    (r/redirect "/")
    (catch Exception e (layout/error "Deletion error" e))))

(defn add!
  [path]
  (if path
    (try
      (logfile/add! path)
      (r/redirect "/")
      (catch Exception e (layout/error "Adding error" e)))
    (layout/error "No Path provided")))

(defroutes routes
  (GET ["/logfile/scan/:id" ] [id] (scan! id))
  (GET ["/logfile/delete/:id" ] [id] (delete! id))
  (POST ["/logfile/add"] {{logfile-path :logfile-path} :params} (add! logfile-path)))
