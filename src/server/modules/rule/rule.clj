(ns ^{:doc "Add rules, get rules provided by the machine learning algorithms, 
			accept these rules by the users and remove rules that doesn't 
			please them."}
	server.modules.rule.rule
	(:require [server.db :as db]
			  [server.notification :as notification]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 								PRIVATE FUNCTIONS							;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- coll-name 
	"Return the collection name for a specific house."
	[houseId] (str "rules_" houseId))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 								PUBLIC FUNCTIONS							;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn new-rules
	"Insert rules into database. These rules are those defined in the App, 
	so are already accepted by the user."
	[obj houseId agentId]
	(if-let [rules (:rules obj)]
		(if (and 
				(every? true? (map #(every? % [:command :controllerId :clauses]) rules))
				(every? true? (map #(every? (:command %) [:nodeId :commandId :value]) rules)))

				(if (every? true? (map  #(db/insert? (coll-name houseId) (assoc % :accepted 1)) rules))
					(notification/notify-new-rules rules)
					{:status 500 :errorMessage "DB did not insert values."}
				)
				{:status 400 :errorMessage "Define nodeId, controllerId, commandId, value and clauses."}
		)
		{:status 400 :errorMessage "Rules not defined"}
	)
)

(defn- helper-get-rules
	"Select type of rules: accepted or not accepted"
	[houseId accepted]
	{:status 200 :rules (into [] (map #(dissoc % :_id :accepted) (db/select (coll-name houseId) {:accepted accepted})))}
)

(defn get-rules 
	"Select rules from db that were generated and already accepted by the users."
	[_ houseId _]
	(helper-get-rules houseId 1)
)

(defn get-learnt-rules
	"Select rules from db that were generated by the Machine Learning algorithms."
	[_ houseId _]
	(helper-get-rules houseId 0)
)

(defn accept-rule
	"Accepted a new rule by the users generated by the Machine Learning algorithms."
	[obj houseId _]
	(let [key-vals {
						"controllerId" (:controllerId obj)
						"command.nodeId" (:nodeId (:command obj))
						"command.commandId" (:commandId (:command obj))
						"command.value" (:value (:command obj))
						"accepted" 1
					}
		  result (db/select (coll-name houseId) key-vals)]
		(if (empty? result)
			(if (db/update? (coll-name houseId) (dissoc key-vals "accepted") :set {:accepted 1})
				{:status 200}
				{:status 500 :errorMessage "DB did not update value."}
			)
			{:status 200 :conflictingRule result}
		)
	)
)

(defn remove-rule
	"Remove a rule from database selected by nodeId, controllerId, commandId and value."
	[obj houseId _]
	(let [key-vals (select-keys obj [:nodeId :controllerId :commandId :value])]
		(if (empty? (db/select (coll-name houseId) key-vals))
			{:status 400 :errorMessage "DB does not contain obj."}
			(if (db/remove? (coll-name houseId) (assoc key-vals :accepted 1))
				{:status 200}
				{:status 500 :errorMessage "DB did not remove value."}
			)
		)
	)
)