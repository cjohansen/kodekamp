(ns kodekamp.db
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [datomic-type-extensions.api :as d]
            [datomic-type-extensions.types :as types]
            [integrant.core :as ig]))

(defmethod types/get-backing-datomic-type :data/edn [_] :db.type/string)

(defmethod types/serialize :data/edn [_ this] (pr-str this))

(defmethod types/deserialize :data/edn [_ ^String s] (read-string s))

(defn create-database [uri]
  (d/create-database uri)
  (let [conn (d/connect uri)]
    @(d/transact conn (edn/read-string {:readers *data-readers*} (slurp (io/resource "schema.edn"))))
    conn))

(defmethod ig/init-key :datomic/conn [_ {:keys [config]}]
  (when-let [uri (:datomic/uri config)]
    (create-database uri)))

(defmethod ig/halt-key! :datomic/conn [_ conn]
  (when conn
    (d/release conn)))
