(ns server.modules.state.state
	(:require
		[server.db :as db]
		[monger.operators :refer :all]
		[clojure.data.json :as json]
		[server.utils :as utils]))

(def ^:private last_states_coll "last_states")

(defn new-data [obj houseId controllerId]
	"Save new data captured by the leafs in the house."
	(if-not (nil? houseId)
		(if-let [data (obj :data)]
			(if (every? true? (map #(and (contains? % :nodeId) (contains? % :dataId) (contains? % :value) (contains? % :timestamp)) data))
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

(defn new-command [obj houseId controllerId]
	"Save new command captured by the leafs in the house."
	(if-not (nil? houseId)
		(if-let [command (obj :command)]
			(if (every? true? (map #(and (contains? % :nodeId) (contains? % :commandId) (contains? % :value) (contains? % :timestamp)) command))
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

(defn new-action [obj houseId agentId]
	"Save new action captured by the actuators in the house."
	(if-not (nil? houseId)
		(if-let [action (obj :action)]
			(if (and (contains? action :controllerId) (contains? action :nodeId) (contains? action :commandId) (contains? action :value))
				(if (and
						(db/insert? (str "all_states_" houseId) (assoc action :agentId agentId))
						(db/update? (str "last_states_" houseId)
									{:controllerId agentId :nodeId (:nodeId action)}
									:set {(keyword (str "command." (:commandId action))) (:value action)}
									:upsert true)
					)
					{:status 200}
					{:status 500 :errorMessage "DB did not insert values."}
				)
				{:status 400 :errorMessage "Define nodeId, commandId and value."}
			)
			{:status 400 :errorMessage "Action not defined"}
		)
		{:status 400 :errorMessage "houseId not defined"}
	)

)

(defn get-house-state [_ houseId _]
	"Get from the house the values of data from sensors and commands from actuators."
	{
		:status 200 
		:state (vec (map #(dissoc % :_id) (db/select (str "last_states_" houseId) {})))
	}
)

(defn notify-action-result[houseId tokens msg]
	"Send a notification to user's device with new action detected."
	(utils/send-notification (tokens houseId) msg))