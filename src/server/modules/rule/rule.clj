(ns server.modules.rule.rule
	(:require [server.db :as db]))

(def ^:private prefix-name "rules_")

(defn- insert [coll-name obj]
	(db/insert coll-name obj))

(defn new-rules [obj houseId]
	"Insert rules into database. These rules are those defined in the App, so are already accepted by the user."
	(if (nil? houseId)
		{:status 400 :errorMessage "houseId not defined"}
		(if-let [rules (:rules obj)]
			(if (every? true? (map #(and (contains? % :nodeId) (contains? % :commandId) (contains? % :value) (contains? % :clauses)) rules))
					(if (every? true? (map #(db/insert? (str prefix-name houseId) (assoc % :accepted true) rules))
						{:status 200}
						{:status 500 :errorMessage "DB did not insert values."})
					{:status 400 :errorMessage "Define nodeId, commandId, value and clauses."})
			{:status 400 :errorMessage "Rules not defined"})))

(defn get-rules [obj houseId]
	)