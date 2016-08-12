(ns server.utils
	(:require [server.db :as db]))

;
;	WEB FUNCTIONS
;

(defn build-response
	"Build response with status and body"
	([status body] 
		{:status status
			:headers {"Content-Type" "text/html"}
			:body body})
	([status] 
		{:status status
			:headers {"Content-Type" "text/html"}}))

(defn session? [request]
	"Verify if a request has session number."
	(if (empty? (:session request))
		(build-response 403 "<h1>FORBIDDEN</h1>")
		(build-response 200 "<h3>Session on</h3>")))

;
;	DATABASE FUNCTIONS
;

(def db-functions {
	"insert" (fn [params] 
		"Insert params from request in the collection specified."
		(db/insert (params "collection") (dissoc (dissoc params "collection") "method"))), 
	"select" (fn [params]
		"params must have a collection name, a key and a value, which will match in the database collection."
		(let [collection (params "collection")
			key (params "key")
			value (params "value")]
			(db/select collection key value)))})