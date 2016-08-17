(ns server.modules.rule.rule
	(:require [server.db :as db]))

(def ^:private prefix-name "rules_")

(defn- insert [coll-name obj]
	(db/insert coll-name obj)
	{:status 200})

(defn new-rules [obj houseId]
	"Insert rules into server. These rules that are defined in the App."
	(if (nil? houseId)
		{:status 400 :errorMessage "houseId not defined"}
		(if-let [rules (:rules obj)]
			(if (every? true? (map 
						#(and (contains? % :nodeId) (contains? % :commandId) (contains? % :value) (contains? % :clauses)) 
						rules))
					(for [rule rules] (insert (str prefix-name houseId) rule))
					{:status 400 :errorMessage "Define nodeId, commandId, value and clauses."})
			{:status 400 :errorMessage "Rules not defined"})))