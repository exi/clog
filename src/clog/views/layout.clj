(ns clog.views.layout
  (:use [hiccup.core :only [html]]
        [hiccup.page :only [doctype include-css include-js]])
  (:require [clj-time.format :as timeformat]
            [clj-time.coerce :as coerce]
            [clojure.string :as string]))

(defn common [title & body]
  (html
   (doctype :html5)
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge,chrome=1"}]
    [:title title]
    (include-css "/vendor/bootstrap-3.0.2/css/bootstrap.min.css")
    (include-css "/css/style.css")
    (include-js "/vendor/jquery-2.0.3/jquery-2.0.3.min.js")
    (include-js "/vendor/bootstrap-3.0.2/js/bootstrap.min.js")
    (include-js "/vendor/d3-3/d3.v3.min.js")
    (include-js "/js/bar-chart.js")
    ]

   [:body
    [:nav {:class "navbar navbar-default" :role "navigation"}
     [:div {:class "navbar-header"}
      [:a {:class "navbar-brand" :href "#"}
       "Clog"]]
     [:div {:class "collapse navbar-collapse"}
      [:ul {:class "nav navbar-nav"}
       [:li
        [:a {:href "/"} "Logs"]]
       [:li
        [:a {:href "/statistics"} "Statistics"]]]]]
    [:div body]
    ]))

(defn container
  [title & body]
  [:div {:class "panel panel-default"}
   [:div {:class "panel-heading"}
    [:h3 {:class "panel-title"} title]]
   [:div {:class "panel-body"}
    body]])

(def datetime-formatter (timeformat/formatter "dd.MM.yyyy hh:mm:ss "))

(defn format-datetime
  [datetime]
  (timeformat/unparse datetime-formatter (coerce/from-long datetime)))

(defn four-oh-four []
  (common "Page Not Found"
          (container "404 Not Found" "The page you requested could not be found!")))

(defn error [title body]
  (common title
          (container title body)))
