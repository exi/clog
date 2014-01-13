(ns clog.database.jdbc
  (:require [clj-time.core :as jtime]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as string])
  (:use clog.database)
  (:import com.mchange.v2.c3p0.ComboPooledDataSource))

(def bulk-insert-size 50000)

(def quotes {:default {:identifier \`}})

(defn pool
  [spec]
  (let [cpds (doto (ComboPooledDataSource.)
               (.setDriverClass (:classname spec)) 
               (.setJdbcUrl (str "jdbc:" (:subprotocol spec) ":" (:subname spec)))
               (.setUser (:user spec))
               (.setPassword (:password spec))
               ;; expire excess connections after 30 minutes of inactivity:
               (.setMaxIdleTimeExcessConnections (* 30 60))
               ;; expire connections after 3 hours of inactivity:
               (.setMaxIdleTime (* 3 60 60)))] 
    {:datasource cpds}))

(defn get-quotes [db] (if (contains? quotes (:subprotocol db))
                        (get quotes (:subprotocol db))
                        (:default quotes)))

(defn quote-identifier [db ident] (jdbc/quoted (:identifier (get-quotes db)) (name ident)))

(defn quote-data [db data]
  (reduce
    conj
    {}
    (map
      (fn [[i v]] [(quote-identifier db i) v])
      data)))

(def logfile-table :logfiles)
(def logfileentries-table :logfileentries)
(def index_runs-table :index_runs)
(def time_cache-table :time_cache)

(defn where-merge
  [acc [sql & v]]
  (if (empty? acc)
    (list* sql v)
    (let [[st & values] acc]
      (list* (string/join " AND " [sql st]) (concat v values)))))

(defn key-pred-value-to-sql [k p v]
  (case pred
    :gteq [(str "(" qk " >= ?)") v]
    :lt [(str "(" qk " < ?)") v]
    :eq [(str "(" qk " = ?)") v]
    "(1 = 1)"))

(defn where-to-sql
  [db where & {:keys [with-where] :or {with-where true}}]
  (let [w (reduce
            where-merge
            []
            (map
              (fn [[k v]]
                (let [qk (quote-identifier db k)]
                  (if (map? v)
                    (reduce
                      where-merge
                      []
                      (map (fn [[pred v]] (key-pred-value-to-sql qk pred v)) v))
                    [(str "(" qk " = ?)") v])))
              where))]
    (if (empty? w)
      {:sql "" :params []}
      {:sql (str (if with-where "WHERE " "") (first w)) :params (rest w)})))

(defrecord JDBCDatabase [db]
  Database
  (get-logfiles [this where]
    (let [w (where-to-sql db where)]
      (jdbc/query
        db
        (vec (list*
               (str "select * from " (quote-identifier db logfile-table)
                    " "
                    (:sql w)
                    " order by id DESC")
               (:params w))))))
  (get-logfileentries [this where]
    (let [w (where-to-sql db where)]
      (jdbc/query
        db
        (vec (list*
               (str "select * from " (quote-identifier db logfileentries-table)
                    " "
                    (:sql w)
                    " order by " (quote-identifier db :datetime)
                    " ASC")
               (:params w))))))
  (count-logfileentries [this where]
    (let [w (where-to-sql db where)]
      (:c (first (jdbc/query
                       db
                       (vec (list*
                              (str "select count(*) as "
                                   (quote-identifier db :c)
                                   " from "
                                   (quote-identifier db logfileentries-table)
                                   " "
                                   (:sql w))
                              (:params w))))))))
  (get-first-logfileentry [this]
    (first (jdbc/query db [(str "select * from "
                                (quote-identifier db logfileentries-table)
                                " order by datetime ASC limit 1")])))
  (get-last-logfileentry [this]
    (first (jdbc/query db [(str "select * from "
                                (quote-identifier db logfileentries-table)
                                " order by datetime DESC limit 1")])))
  (get-logfileenty-datetimes-inserted-after [this start]
    (map
      #(get % :datetime)
      (jdbc/query db [(str "select "
                         (quote-identifier db :datetime)
                         " from "
                         (quote-identifier db logfileentries-table)
                         " where inserted >= ? group by "
                         (quote-identifier db :datetime)
                         " order by "
                         (quote-identifier db :datetime)
                         " ASC")
                    start])))
  (get-last-index-run-datetime [this]
    (:datetime (first
                 (jdbc/query db [(str "select "
                                      (quote-identifier db :datetime)
                                      " from "
                                      (quote-identifier db index_runs-table)
                                      " order by "
                                      (quote-identifier db :datetime)
                                      " DESC limit 1")]))))
  (get-cache-block [this where]
    (let [w (where-to-sql db where)]
      (first (jdbc/query
               @db
               (vec (list*
                      (str "select * from " (quote-identifier db time_cache-table)
                           " "
                           (:sql w)
                           " limit 1")
                      (:params w)))))))
  (save-logfile! [this logfile]
    (jdbc/insert! db logfile-table logfile :entities #(quote-identifier db %)))
  (save-logfileentries! [this entries]
    (loop [e entries]
      (when (seq e)
        (do (apply jdbc/insert!
                   (list* db logfileentries-table
                          (concat
                            (take bulk-insert-size e)
                            [:entities #(quote-identifier db %)])))
            (recur (drop bulk-insert-size e))))))
  (save-index-run-at! [this datetime]
    (jdbc/insert! db index_runs-table {:datetime datetime} :entities #(quote-identifier db %)))
  (save-cache-block! [this block]
    (jdbc/insert! db time_cache-table block :entities #(quote-identifier db %)))
  (update-logfile! [this logfile]
    (let [w (where-to-sql db {:id (:id logfile)} :with-where false)]
      (jdbc/update!
        db
        logfile-table
        (dissoc logfile :id) (list* (:sql w) (:params w)) :entities #(quote-identifier db %))))
  (update-cache-block! [this block]
    (let [w (where-to-sql db {:id (:id block)} :with-where false)]
      (jdbc/update!
        db
        time_cache-table
        (dissoc block :id) (list* (:sql w) (:params w)) :entities #(quote-identifier db %))))
  (delete-logfiles! [this where]
    (let [w (where-to-sql db where :with-where false)]
      (jdbc/delete! db logfile-table (list* (:sql w) (:params w)))))
  (delete-logfileentries! [this where]
    (let [w (where-to-sql db where :with-where false)]
      (jdbc/delete! db logfileentries-table (list* (:sql w) (:params w))))))

(defn create [jdbc-config] (JDBCDatabase. (pool jdbc-config)))
