(ns server.db
    (:require 
		(monger [core :as mg] [collection :as mc] [result :as res] [operators :as op]))
	(:import 
		[com.mongodb MongoOptions ServerAddress DB WriteConcern]
		[org.bson.types ObjectId]
        [java.util.logging Logger Level])
    (:refer-clojure :exclude [update remove]))


; Hide MongoDB annoying logs
(. (. Logger getLogger "org.mongodb.driver") setLevel (. Level SEVERE))


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


(defn server.db/update [coll-name conditions &{:keys [set] :or {set {}}}]
    "Updates documents"
    (mc/update db coll-name conditions {op/$set set}))

(defn remove [coll-name conditions]
    "Remove documents"
    (mc/remove db coll-name conditions))


(defn select [coll-name map-key-value]
	"Receive a collection name and a map. This map has the values to use as filter.
	Returns a Clojure map with map from DB."
	(mc/find-maps db coll-name map-key-value))

