 (ns clog.views.logfiles
   (:require [clog.views.layout :as layout]))

(defn
  view
  [logfiles]
  [:div
    (layout/container
      "Logfiles"
      [:table {:class "table table-hover"}
       [:thead
        [:tr
         [:th "ID"]
         [:th "Path"]
         [:th "Lines"]
         [:th "Last Scan"]
         [:th]
         ]]
       [:tbody
        (map
          (fn [{id :id
                path :path
                lines :lines
                last-scan :last_scan}]
            [:tr
             [:td id]
             [:td path]
             [:td (if lines lines "")]
             [:td (if last-scan (layout/format-datetime last-scan) "")]
             [:td
              [:a {:role "button" :class "btn btn-primary" :href (str "/logfile/scan/" id)} "Scan"]
              [:a {:role "button" :class "btn btn-danger" :href (str "/logfile/delete/" id)} "Delete"]
              ]])
          logfiles)]]
      [:form {:role "form" :method "POST" :action "/logfile/add"}
       [:h3 "Add Logfile"]
       [:div {:class "form-group"}
        [:label {:for "logfile-path" :class "col-sm-1 control-label"} "Path"]
        [:div {:class "col-sm-13"}
         [:input {:name "logfile-path" :placeholder "Pfad"}] ] ]
       [:button {:type "submit" :class "btn btn-primary"} "Add"]])
    (layout/container
      "Indexing"
      [:a {:role "button" :class "btn btn-primary" :href (str "/indexer/run")} "Index All"])])
