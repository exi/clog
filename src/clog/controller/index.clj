(ns clog.controller.index
  (:use [compojure.core :only [defroutes GET POST]])
  (:require [clojure.string :as str]
            [ring.util.response :as ring]
            [clog.models.logfiles :as logfiles]
            [clog.views.index :as view]))

(defn index
  []
  (view/view (logfiles/all))
  )

(defroutes routes
  (GET "/" [] (index)))
