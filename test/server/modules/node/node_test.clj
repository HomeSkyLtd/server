(ns server.modules.node.node-test
  (:use [clojure.test])
  (:use [server.modules.node.node])
  (:require [server.db :as db])
  )

;
; Check if all function exists
;

(deftest test-new-detected-nodes 
     (println (new-detected-nodes {:node {"nodeId" 1 "nodeClass" 1}} 0)))

