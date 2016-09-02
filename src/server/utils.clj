(ns server.utils
	(:require 	[server.db :as db]
			  	[clojure.data.json :as json]
			  	[clj-http.client :as client]))

(def thread-pool (agent '()))

(defn find-keys [map value]
	"Returns the occurrences of the key related to value in map"
	(let [values
			(for [pair map]
				(if (= (second pair) value)
					(first pair)
				)
			)]
		(filter #(not (nil? %)) values)
	)
)


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

(defn- build-msg [token msg]
	(str "{\"notification\": {\"body\":\"" msg "\"},\"to\":\"" token "\"}"))

(defn- send-on-thread [tokens msg]
	(let [auth-key "key=AIzaSyClArUOQgE1rH2ff3DELo6vvmQuWTZ68QA"]
		(map #(client/post "https://fcm.googleapis.com/fcm/send"
			{:body (build-msg % msg)
			 :headers {"Authorization" auth-key "Content-Type" "application/json"}
			}) tokens
		)
	)
)

(defn send-notification [tokens msg]
	(await-for 1000 (send thread-pool concat (send-on-thread tokens msg))))

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
				(db/insert (str "node_" controllerId) (json/read-str description)))))

	"selectData" (fn [params controllerId]
		"Selects data from node with id = nodeId"
			(db/select (str "data_" controllerId) "nodeId" (Integer. (params "nodeId"))))

	"selectNode" (fn [params controllerId]
		"Selects description from node with id = nodeId"
			(db/select (str "node_" controllerId) "id" (Integer. (params "nodeId"))))})


(defn- insert [params controllerId]
	"Calls specific insert function depending on data from request."
		(cond
			(contains? params "data")
				((db-functions "insertData") params controllerId)

			(contains? params "xcommand")
				((db-functions "insertXCommand") params controllerId)

			(contains? params "description")
				((db-functions "insertNode") params controllerId)

			:else "No data, nor external commando, nor node id. Nothing done."))

(defn- select [params controllerId]
	(let [target (params "target")]
		(case target
			"data" ((db-functions "selectData") params controllerId)
			"node" ((db-functions "selectNode") params controllerId)
			"Target not specified: data or node")))

(defn call-db-function [function params]
	(if-let [controllerId (params "controllerId")]
		(case function
			"insert" (if-let [result (insert params controllerId)] result "Ok")
			"select" (select params controllerId))
		"No controller id. Nothing done."))
