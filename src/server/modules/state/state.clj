(ns server.modules.state.state
	(:require
		[server.db :as db]
		[monger.operators :refer :all]))

(def ^:private last_states_coll "last_states")

(defn new-data [obj houseId controllerId]
	"Save new data captured by the leafs in the house."
	(if-not (nil? houseId)
		(if-let [data (obj :data)]
			(if (every? true? (map #(and (contains? % :nodeId) (contains? % :dataId) (contains? % :value) (contains? % :timestamp)) data))
				(if (and ;TODO CONTINUAR O DB UPDATE PARA O NEW COMMAND E PARA O NEW ACTION
						(every? true? (map #(db/insert? (str "all_states_" houseId) (assoc % :controllerId controllerId)) data))
						(db/update last_states_coll (map #(merge % (db/select last_states_coll {:houseId houseId})) data) :upsert true)
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
						(every? true? (map #(db/insert? (str "last_state_" houseId) (assoc % :controllerId controllerId)) command)))
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
			(if (every? true? (map #(and (contains? % :nodeId) (contains? % :commandId) (contains? % :value)) action))
				(if (and
						(every? true? (map #(db/insert? (str "all_states_" houseId) (assoc % :agentId agentId)) action))
						(every? true? (map #(db/insert? (str "last_state_" houseId) (assoc % :agentId agentId)) action)))
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
	{:status 200 :state 
		(let [coll-name (str "last_state_" houseId)] 
		(apply vector 
			(map (fn [id] 
				(assoc {} 
					:nodeId id
					:controllerId (:controllerId (db/select coll-name {:nodeId id} :one true))
					:data (into [] (map #(dissoc % :_id :nodeId :controllerId) (filter (fn [m] (= (m :nodeId) id)) (db/find-rows-with-key coll-name :dataId))))
					:command (into [] (map #(dissoc % :_id :nodeId :controllerId) (filter (fn [m] (= (m :nodeId) id)) (db/find-rows-with-key coll-name :commandId))))))
			(db/select-distinct coll-name :nodeId)))
		)
	}
)