(ns server.db
	(:require 
		[monger.core :as mg]
		[monger.collection :as mc])
	(:import 
		[com.mongodb MongoOptions ServerAddress DB WriteConcern]
		[org.bson.types ObjectId]))

(defn db
	"Get Database by host and port. All parameters are optional."
	([host port db-name]
		 (mg/get-db (mg/connect {:host host :port port}) db-name))
	([host db-name]
		 (mg/get-db (mg/connect {:host host}) db-name))
	([db-name]
		 (mg/get-db (mg/connect) db-name))
	([]
		 (mg/get-db (mg/connect) "server-db")))

(defn insert [obj coll-name]
	"Insert an object (could be a map, a string, etc.) in the collection specified."
	(let [db (db)]
		(mc/insert db coll-name (assoc obj :_id (ObjectId.)))))