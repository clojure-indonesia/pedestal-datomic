(ns pedestal-datomic.datomic
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [datomic.client.api :as d]))

(defn- read-edn
  [filename]
  (if-let [r (io/resource (str "datomic/" filename))]
    (edn/read-string (slurp r))
    (throw (RuntimeException. (str "You need to add a resource datomic/" filename)))))

(def get-client
  (memoize #(d/client (read-edn "config.edn"))))

(defn get-connection
  [client db-name]
  (d/connect client {:db-name db-name}))

(defn- has-ident?
  [db ident]
  (contains? (d/pull db {:eid ident :selector [:db/ident]})
             :db/ident))

(defn- fresh-db?
  [db]
  (not (has-ident? db :movie/title)))

(defn load-dataset
  [conn]
  (let [db (d/db conn)]
    (when (fresh-db? db)
      (let [xact #(d/transact conn {:tx-data %})
            schema (read-edn "schema.edn")
            data (read-edn "data.edn")]
        (xact schema)
        (xact data)))))
