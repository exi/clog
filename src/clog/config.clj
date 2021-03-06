(ns clog.config
  (:require [clog.database.jdbc :as jdbc])
  )

;(def db {
;         :classname "org.postgresql.driver"
;         :subprotocol "postgresql"
;         :subname "//localhost:5432/clog"
;         :user "root"
;         :password "root"
;         })
;(def db (mongodb/create))
(def db (jdbc/create {:classname "com.mysql.jdbc.Driver"
                      :subprotocol "mysql"
                      :subname "//127.0.0.1:3306/clog"
                      :user "root"
                      :password "root"}))

(def listen-port 3000)

(def cache-resolutions (vec (map #(int (* 30000 (Math/pow 4 %))) (range 7)))) ;must be sorted ASC
