(ns server.db
	(:require 
		[monger.core :as mg]
		[monger.collection :as mc])
	(:import 
		[com.mongodb MongoOptions ServerAddress DB WriteConcern]
		[org.bson.types ObjectId]))

(defn init-db
	"Create Database by host and port. All parameters are optional."
	([host port db-name]
		 (def ^:private db (mg/get-db (mg/connect {:host host :port port}) db-name)))
	([host db-name]
		 (def ^:private db (mg/get-db (mg/connect {:host host}) db-name)))
	([db-name]
		 (def ^:private db (mg/get-db (mg/connect) db-name)))
	([]
		 (def ^:private db (mg/get-db (mg/connect) "server-db"))))

(defn no-db? []
	"Return true if the db is not initialized yet."
	(nil? (resolve 'db)))

(defn insert [obj coll-name]
	"Insert an object (could be a map, a string, etc.) in the collection specified."
	(mc/insert db coll-name (assoc obj :_id (ObjectId.))))