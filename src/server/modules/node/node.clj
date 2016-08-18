(ns server.modules.node.node
    (:require [server.db :as db]
              (monger [core :as mg] [result :as res])
              [validateur.validation :refer :all])
    )

(defn- validate-nodes
    [nodes]
    
    (let [valid-node (validation-set
                        (presence-of :nodeId :message "Missing field"))]
        (reduce #(conj % (valid-node %2)) {} nodes))) 

(defn- node-exists?
    [house-id controller-id node-id]
    (= 1 (count (db/select (str "node_" house-id) {:controllerId controller-id :nodeId node-id}))))

(defn- get-node
    [house-id controller-id node-id]
    (db/select (str "node_" house-id) {:controllerId controller-id :nodeId node-id} :one true))

(defn- error-message
    "Transforms an error-map into a string of the error"
    [error-map]
        (str (first (first (vals (last error-map)))) ": " (subs (str (first (keys (last error-map)))) 1))
    )

(defn new-detected-nodes 
    "Function to save new detected nodes and notify users"
    [obj house-id controller-id]
    (let [valid ((validate-some 
                (all-keys-in #{:node} :unknown-message "Invalid key")
                (presence-of :node :message "Missing field")
                (validate-by :node sequential? :message "Field is not a list")
                (validate-by :node #(not (empty? %)) :message "Field is empty")
                ) obj)]
        ;Check if obj is valid
        (if (first valid) 
            (let [nodes (:node obj) valid-nodes (validate-nodes (:node obj))]
                ;Check if node list is valid
                (if (empty? valid-nodes)
                    ;Check if node already exists
                    (if (not-any? #(node-exists? house-id controller-id (:nodeId %)) nodes)
                        (if (db/insert? (str "node_" house-id) 
                        (map #(assoc % :controllerId controller-id :accept 0) nodes))
                            ; TODO: Notify users of detected nodes
                            {:status 200}
                            ; Return error
                            {:status 500 :errorMessage "Database error: Couldn't insert"}
                        )
                        {:status 400 :errorMessage "Some node is already in the database" }
                    )
                    {:status 400 :errorMessage (error-message [valid-nodes]) }
                )
            )
            {:status 400 :errorMessage (error-message valid) }
            )))


(defn set-node-extra
    "Set extra info of node (like name, room etc)"
    [obj house-id user-id]
    (let [valid ((validate-some 
                (all-keys-in #{:nodeId :controllerId :extra} :unknown-message "Invalid key")
                (presence-of #{:nodeId :controllerId :extra} :message "Missing field")
                ) obj)]
        (if (first valid)
            (let [node (get-node house-id (:controllerId obj) (:nodeId obj))]
                (if (nil? node)
                    {:status 400 :errorMessage "Tried to set extra of non existent node" }
                    (if (res/acknowledged? 
                    (db/update (str "node_" house-id) (select-keys obj [:controllerId :nodeId]) 
                    :set (into {} (map #(conj {} [(keyword (str "extra." (subs (str (get % 0)) 1))) (get % 1)]) (:extra obj)))))
                        {:status 200 }
                        {:status 400 :errorMessage "Database error: Couldn't set node extra"})))
                
            {:status 400 :errorMessage (error-message valid)})))


(defn get-nodes-info
    "Get nodes info"
    [obj house-id user-id]
    {:status 501})


(defn accept-node
    "Accept or rejects detected node"
    [obj house-id user-id]
    (let [valid ((validate-some 
                (all-keys-in #{:nodeId :controllerId :accept} :unknown-message "Invalid key")
                (presence-of #{:nodeId :controllerId :accept} :message "Missing field")
                ) obj)]
        (if (first valid)
            (let [node (get-node house-id (:controllerId obj) (:nodeId obj))]
                (if (nil? node)
                    {:status 400 :errorMessage "Tried to accept non existent node"}
                    (if (= (:accept node) 0)
                        (if (res/acknowledged? (if (= (:accept obj) 1)
                                (db/update (str "node_" house-id) (select-keys obj [:controllerId :nodeId]) :set {:accept 1})
                                (db/remove (str "node_" house-id) (select-keys obj [:controllerId :nodeId]))))
                            {:status 200 }
                            {:status 500 :errorMessage "Database error: Couldn't accept node" })
                        {:status 400 :errorMessage "Tried to accept accepted node" })))
            {:status 400 :errorMessage (error-message valid)})))


(defn set-node-state
    "Set node state (as dead or alive)"
    [obj house-id controller-id]
    {:status 501})
