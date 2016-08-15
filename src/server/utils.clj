(ns server.utils
	(:require 	[server.db :as db]
			  	[clojure.data.json :as json]))

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

(def ^:private db-functions {
	"insertData" (fn [params controllerId]
		"Insert data from request in the Data collection of controller specified."
		(let [data (params "data")]
			(if-not (nil? data)
				(db/insert (str "data_" controllerId) (json/read-str data))))),

	"insertXCommand" (fn [params controllerId]
		"Insert external command in the Data collection of controller specified."
		(let [xcommand (params "xcommand")]
			(if-not (nil? xcommand)
				(db/insert (str "data_" controllerId) (json/read-str xcommand))))),

	"insertNode" (fn [params controllerId] 
		"Insert params from request in the Nodes collection (sensor, actuator and controller) of the controller specified."
		(let [description (params "description")]
			(if-not (nil? description)
				(db/insert (str "nodes_" controllerId) (json/read-str description)))))

	;"select" (fn [params controllerId]
	;	"params must have a collection name, a key and a value, which will match in the database collection."
	;	(let [key (params "key")
	;		value (params "value")]
	;		(db/select collection key value)))
	})

(defn- insert [params]
	"Calls specific insert function depending on data from request."
	(let [controllerId (params "controllerId")]
		(if-not (nil? controllerId)
			(if (contains? params "data")
				((db-functions "insertData") params controllerId)
				(if (contains? params "xcommand")
					((db-functions "insertXCommand") params controllerId)
					(if (contains? params "description")
						((db-functions "insertNode") params controllerId)
						"No data, nor external commando, nor node id. Nothing done.")))
			"No controller id. Nothing done.")))

(defn call-db-function [function params]
	(if (= function "insert") 
		(if-let [result (insert params)] result "Ok")))