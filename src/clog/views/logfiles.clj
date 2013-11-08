 (ns clog.views.logfiles
   (:require [clog.views.layout :as layout]))

(defn
  view
  [logfiles]
  (layout/container
    "Logfiles"
    [:table {:class "table table-hover"}
     [:thead
      [:tr
       [:th ""]
       [:th "Filename"]
       [:th "Lines"]
       [:th "Last Access"]
       [:th]
       ]]
     [:tbody
      (map
        (fn [{id :id
              filename :filename
              lines :lines
              lastaccess :last_access}]
          [:tr
           [:td id]
           [:td filename]
           [:td (if lines lines "")]
           [:td (if lastaccess (layout/format-datetime lastaccess) "")]
           [:td
            [:a {:role "button" :class "btn btn-primary" :href (str "/logfile/scan/"id)} "Scan"]]])
        logfiles)]]))
