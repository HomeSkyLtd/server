(ns ^{:doc "Monitors new data and command read by the sensors and actuators, 
			or actions executed by the users over the actuators."}
	server.modules.state.state
	(:require
		[server.db :as db]
		[monger.operators :refer :all]
		[clojure.data.json :as json]
		[server.notification :as notification]))

(def ^:private last_states_coll "last_states")

(defn new-data 
	"Save new data captured by the leafs in the house."
	[obj houseId controllerId]
	(if-not (nil? houseId)
		(if-let [data (obj :data)]
			(if (every? true? (map #(every? % [:nodeId :dataId :value :timestamp]) data))
				(if (and
						(every? true? (map #(db/insert? (str "all_states_" houseId) (assoc % :controllerId controllerId)) data))
						(every? true? 
							(map 
								#(db/update? (str "last_states_" houseId)
									{:controllerId controllerId :nodeId (:nodeId %)}
									:set { (keyword (str "data." (:dataId %))) (:value %) }
									:upsert true
								)
								data
							)
						)
					)
					{:status 200}
					{:status 500 :errorMessage "DB did not insert values."}
				)
				{:status 400 :errorMessage "Define nodeId, dataId, value and timestamp."}
			)
			{:status 400 :errorMessage "Data not defined"}
		)
		{:status 400 :errorMessage "houseId not defined"}
	)

)

(defn new-command
	"Save new command captured by the leafs in the house."
	[obj houseId controllerId]
	(if-not (nil? houseId)
		(if-let [command (obj :command)]
			(if (every? true? (map #(every? % [:nodeId :commandId :value :timestamp]) command))
				(if (and 
						(every? true? (map #(db/insert? (str "all_states_" houseId) (assoc % :controllerId controllerId)) command))
						(every? true? 
							(map 
								#(db/update? (str "last_states_" houseId)
									{:controllerId controllerId :nodeId (:nodeId %)}
									:set { (keyword (str "command." (:commandId %))) (:value %) }
									:upsert true
								)
								command
							)
						)
					)
					{:status 200}
					{:status 500 :errorMessage "DB did not insert values."}
				)
				{:status 400 :errorMessage "Define nodeId, commandId, value and timestamp."}
			)
			{:status 400 :errorMessage "Command not defined"}
		)
		{:status 400 :errorMessage "houseId not defined"}
	)

)

(defn new-action
	"Save new action captured by the actuators in the house."
	[obj houseId agentId]
	(if-not (nil? houseId)
		(if-let [action (obj :action)]
			(if (every? action [:controllerId :nodeId :commandId :value])
				(if (and
						(db/insert? (str "all_states_" houseId) (assoc action :agentId agentId))
						(db/update? (str "last_states_" houseId)
									{:controllerId (:controllerId action) :nodeId (:nodeId action)}
									 :set {(keyword (str "command." (:commandId action))) (:value action)}
									 :upsert true))
					(notification/notify-new-action action)
					{:status 500 :errorMessage "DB did not insert values."}
				)
				{:status 400 :errorMessage "Define nodeId, commandId and value."}
			)
			{:status 400 :errorMessage "Action not defined"}
		)
		{:status 400 :errorMessage "houseId not defined"}
	)

)

(defn get-house-state
	"Get from the house the values of data from sensors and commands from actuators."
	[_ houseId _]
	{
		:status 200 
		:state (vec (map #(dissoc % :_id) (db/select (str "last_states_" houseId) {})))
	}
)

(defn send-action-result
	"Receive the result of an action (done or couldn't be done) from the controller"
	[obj houseId agentId]
	(notification/notify-action-result (update obj :action #(assoc % :controllerId agentId)) houseId)
)