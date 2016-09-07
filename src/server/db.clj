(ns server.db
    (:require
		(monger [core :as mg] [collection :as mc] [result :as res] [operators :as op]))
	(:import
		[com.mongodb MongoOptions ServerAddress DB WriteConcern]
		[org.bson.types ObjectId])
    (:refer-clojure :exclude [update remove]))


(defn- init-db
	"Create Database by host and port. All parameters are optional."
	([host port db-name]
		(mg/get-db (mg/connect {:host host :port port}) db-name))
	([host db-name]
		(mg/get-db (mg/connect {:host host}) db-name))
	([db-name]
		(mg/get-db (mg/connect) db-name))
	([]
		(mg/get-db (mg/connect) "server-db")))


; Database Connection singleton
(def db (init-db))


(defn insert [coll-name obj &{:keys [return-inserted] :or {return-inserted false}}]
	"Insert an object (could be a map, a string, etc.) in the collection specified."
	(if (map? obj)
		(if return-inserted
			(mc/insert-and-return db coll-name (assoc obj :_id (ObjectId.)))
			(mc/insert db coll-name (assoc obj :_id (ObjectId.)))
		)
		(mc/insert-batch db coll-name obj)
	)
)

(defn insert? [coll-name obj]
    "Same as insert, but returns true if insert was ok and false otherwise"
    (res/acknowledged? (insert coll-name obj)))


(defn server.db/update [coll-name conditions
    &{:keys [set add-to-set multi upsert] :or {set {} add-to-set {} multi true upsert false}}]
    "Updates documents"
    (let [args-map {op/$set set op/$addToSet add-to-set}]
        (mc/update db coll-name conditions (into {} (filter #(not (empty? (get % 1))) args-map)) {:multi multi :upsert upsert})))

(defn server.db/update? [coll-name conditions
    &{:keys [set add-to-set multi upsert] :or {set {} add-to-set {} multi true upsert false}}]
    (res/acknowledged? (update coll-name conditions :set set :add-to-set add-to-set :multi multi :upsert upsert)))

(defn remove
    "Remove documents"
    ([coll-name conditions]
        (mc/remove db coll-name conditions))
    ([coll-name]
        (mc/remove db coll-name)))

(defn remove?
	"Acknowledged the remove operation"
	([coll-name conditions]
        (res/acknowledged? (remove coll-name conditions)))
    ([coll-name]
        (res/acknowledged? (remove coll-name))))


(defn select [coll-name map-key-value &{:keys [one] :or {one false}}]
	"Receive a collection name and a map. This map has the values to use as filter.
	Returns a Clojure map with map from DB."
    (if one
        (mc/find-one-as-map db coll-name map-key-value)
	    (mc/find-maps db coll-name map-key-value)))

(defn select-distinct [coll-name str-key]
	"Find distinct values for a key."
	(mc/distinct db coll-name str-key))

(defn find-rows-with-key [coll-name key]
	(mc/find-maps db coll-name {key {op/$exists true}}))
