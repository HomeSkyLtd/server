(ns server.modules.node.node
    (:require [server.db :as db])
    )


(defn new-detected-nodes 
    "Function to save new detected nodes and notify users"
    [obj house-id]
    (db/insert (str "node_" house-id) (:node obj)))