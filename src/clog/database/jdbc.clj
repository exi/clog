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
  [join-string acc [sql & v]]
  (if (empty? acc)
    (list* sql v)
    (let [[st & values] acc]
      (list* (str "(" (string/join join-string [sql st]) ")") (concat v values)))))

(def where-and-merge (partial where-merge " AND " ))
(def where-or-merge (partial where-merge " OR " ))

(defn key-predicate-value-to-sql [k predicate v]
  (case predicate
    :gteq [(str "(" k " >= ?)") v]
    :lt [(str "(" k " < ?)") v]
    :eq [(str "(" k " = ?)") v]
    "(1 = 1)"))

(defn handle-multiple-predicates [k predicate-map]
  (map (fn [[p v]] (key-predicate-value-to-sql k p v)) predicate-map))

(declare handle-where-tree)
(defn handle-or-list [db l]
  (map (fn [tree] (handle-where-tree db tree)) l))

(defn handle-where-tree-node [db k v]
  (let [qk (quote-identifier db k)]
    (if (map? v)
      (reduce where-and-merge [] (handle-multiple-predicates qk v))
      (if (= k :$or)
        (reduce where-or-merge [] (handle-or-list db v))
        [(str "(" qk " = ?)") v]))))

(defn handle-where-tree [db tree]
  (reduce where-and-merge [] (map (fn [[k v]] (handle-where-tree-node db k v)) tree)) )

(defn where-tree-to-sql-list
  [db where]
  (handle-where-tree db where))

(defn where-to-sql
  [db where & {:keys [with-where] :or {with-where true}}]
  (let [where-list (where-tree-to-sql-list db where)]
    (if (empty? where-list)
      {:sql "" :params []}
      {:sql (str (if with-where "WHERE " "") (first where-list)) :params (rest where-list)})))

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
  (get-cache-blocks [this where]
    (let [w (where-to-sql db where)]
      (jdbc/query
        db
        (vec (list*
               (str "select * from " (quote-identifier db time_cache-table)
                    " "
                    (:sql w))
               (:params w))))))
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
