(ns server.modules.node.node
    (:require [server.db :as db]
              (monger [core :as mg] [result :as res])
              [validateur.validation :refer :all])
    )

(defn validate-nodes
    [nodes]
      {}
    )

(defn new-detected-nodes 
    "Function to save new detected nodes and notify users"
    [obj house-id controller-id]
    (let [valid ((validation-set 
                (presence-of :node)

                ) obj)]
        (if (empty? valid) 
            (if (db/insert? (str "node_" house-id) (map #(assoc % :controllerId controller-id) (:node obj)))
                ; TODO: Notify users of detected nodes
                {:status 200}
                ; Return error
                {:status 500 :errorMessage "Database error: Couldn't insert"}
            )
            {:status 400 :errorMessage "Invalid input"})))


(defn set-node-extra
    "Set extra info of node (like name, room etc)"
    [obj house-id user-id]
    {:status 501})


(defn get-nodes-info
    "Get nodes info"
    [obj house-id user-id]
    {:status 501})


(defn accept-node
    "Accept or rejects detected node"
    [obj house-id user-id]
    {:status 501})


(defn set-node-state
    "Set node state (as dead or alive)"
    [obj house-id user-id]
    {:status 501})
