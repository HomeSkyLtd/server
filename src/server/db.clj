(ns server.db
	(:require 
		[monger.core :as mg]
		[monger.collection :as mc])
	(:import 
		[com.mongodb MongoOptions ServerAddress DB WriteConcern]
		[org.bson.types ObjectId]
        [java.util.logging Logger Level]))

(defn- hide-db-log
    "Hide mongodb logs"
    []
    (. (. Logger getLogger "org.mongodb.driver") setLevel (. Level SEVERE)))


(defn init-db
	"Create Database by host and port. All parameters are optional."
	([host port db-name]
        (hide-db-log)
		(def ^:private db (mg/get-db (mg/connect {:host host :port port}) db-name)))
	([host db-name]
        (hide-db-log)
		(def ^:private db (mg/get-db (mg/connect {:host host}) db-name)))
	([db-name]
        (hide-db-log)
		(def ^:private db (mg/get-db (mg/connect) db-name)))
	([]
        (hide-db-log)
		(def ^:private db (mg/get-db (mg/connect) "server-db"))))

(defn- no-db? []
	"Return true if the db is not initialized yet."
	(nil? (resolve 'db)))

(defn insert [coll-name obj]
	"Insert an object (could be a map, a string, etc.) in the collection specified."
	;(if (no-db?) (init-db))
	(if (map? obj) 
		(mc/insert db coll-name (assoc obj :_id (ObjectId.)))
		(mc/insert-batch db coll-name obj))
	)

(defn select [coll-name key value]
	"Receive a collection name, a key and a value. Returns a Clojure map with map from DB."
	;(if (no-db?) (init-db))
	(mc/find-maps db coll-name {key value}))

(init-db)