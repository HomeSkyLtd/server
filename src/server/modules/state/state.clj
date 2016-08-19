(ns server.modules.state.state
	(:require
		[server.db :as db]
		[monger.operators :refer :all]))

(defn new-data [obj houseId _]
	"Save new data captured by the leafs in the house."
	(if-not (nil? houseId)
		(if-let [data (obj :data)]
			(if (every? true? (map #(and (contains? % :nodeId) (contains? % :dataId) (contains? % :value) (contains? % :timestamp)) data))
				(if (and 
						(every? true? (map #(db/insert? (str "all_states_" houseId) %) data))
						(every? true? (map #(db/insert? (str "last_state_" houseId) %) data)))
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

(defn new-command [obj houseId _]
	"Save new command captured by the leafs in the house."
	(if-not (nil? houseId)
		(if-let [command (obj :command)]
			(if (every? true? (map #(and (contains? % :nodeId) (contains? % :commandId) (contains? % :value) (contains? % :timestamp)) command))
				(if (and 
						(every? true? (map #(db/insert? (str "all_states_" houseId) %) command))
						(every? true? (map #(db/insert? (str "last_state_" houseId) %) command)))
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

(defn new-action [obj houseId _]
	"Save new action captured by the actuators in the house."
	(if-not (nil? houseId)
		(if-let [action (obj :action)]
			(if (every? true? (map #(and (contains? % :nodeId) (contains? % :commandId) (contains? % :value)) action))
				(if (and
						(every? true? (map #(db/insert? (str "all_states_" houseId) %) action))
						(every? true? (map #(db/insert? (str "last_state_" houseId) %) action)))
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
	{:status 200 :state (let [coll-name (str "last_state_" houseId)] 
					(apply vector 
						(map 
							(fn [id] (assoc {} :nodeId id
								:data (into [] (map #(dissoc % :_id :nodeId) (filter (fn [m] (= (m :nodeId) id)) (db/find-rows-with-key coll-name :dataId))))
								:command (into [] (map #(dissoc % :_id :nodeId ) (filter (fn [m] (= (m :nodeId) id)) (db/find-rows-with-key coll-name :commandId))))
									)
							)
							(db/select-distinct coll-name :nodeId)
						)
					)
				)
	}
)