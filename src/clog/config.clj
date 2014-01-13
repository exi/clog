(ns clog.config
  (:require [clog.database.memory :as memdb])
  (:require [clog.database.jdbc :as jdbc])
  )

;(def db {
;         :classname "org.postgresql.driver"
;         :subprotocol "postgresql"
;         :subname "//localhost:5432/clog"
;         :user "root"
;         :password "root"
;         })
;(def db (memdb/create))
(def db (jdbc/create {:classname "com.mysql.jdbc.Driver"
                      :subprotocol "mysql"
                      :subname "//127.0.0.1:3306/clog"
                      :user "root"
                      :password "root"}))

(def cache-resolutions (vec (map #(int (* 1000 (Math/pow 4 %))) (range 7)))) ;must be sorted ASC
