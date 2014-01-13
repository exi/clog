(ns clog.database.memory
  (:require [clj-time.core :as jtime])
  (:use clog.database))

(defn where-filter
  [where]
  (fn [entry]
    (reduce
      (fn [acc [k v]]
        (let [o-value (get entry k)]
          (if (map? v)
            (reduce
              (fn [acc [pred v]]
                (case pred
                  :gteq (and acc (>= o-value v))
                  :lt (and acc (< o-value v))
                  :eq (and acc (= o-value v))
                  false))
              true
              v)
            (if (= v o-value)
              acc
              false))))
      true
      where)))

(defn not-where-filter [where]
  (comp not (where-filter where)))

(defn gen-id
  [counter]
  (swap! counter inc))

(defrecord MemoryDatabase [db fileid entryid]
  Database
  (get-logfiles [this where] (filter (where-filter where) (get @db :logfiles)))
  (get-logfileentries [this where] (filter (where-filter where) (get @db :logfileentries)))
  (count-logfileentries [this where] (count (get-logfileentries this where)))
  (save-logfile! [this logfile] (let [id (gen-id fileid)]
                                 (swap!
                                   db
                                   #(assoc % :logfiles (conj (:logfiles %) (assoc logfile :id id))))))
  (save-logfileentries! [this entries] (doseq [entry entries]
                                        (let [id (gen-id entryid)]
                                          (swap!
                                            db
                                            #(assoc % :logfileentries
                                                    (conj (:logfileentries %) (assoc entry :id id)))))))
  (update-logfile! [this logfile] (swap!
                                    db
                                    (fn [db]
                                      (assoc db :logfiles
                                             (let [without-old (remove
                                                                 (where-filter {:id (:id logfile)})
                                                                 (:logfiles db))]
                                               (conj without-old logfile))))))
  (delete-logfiles! [this where] (swap!
                                     db
                                     #(assoc % :logfiles
                                             (remove (where-filter where) (:logfiles %))))))

(defn datetime-sort
  [a b]
  (let [c (compare (:datetime a) (:datetime b))]
    (if (not= c 0)
      c
      (compare (:id a) (:id b)))))

(defn create-container [] (sorted-set-by datetime-sort))
(defn create [] (MemoryDatabase. (atom {:logfiles (create-container) :logfileentries (create-container)})
                                 (atom 0)
                                 (atom 0)))
