(ns server.modules.node.node-test
  (:use [clojure.test])
  (:use [server.modules.node.node])
  (:require [server.db :as db])
  )

;
; Check if all function exists
;

(deftest test-new-detected-nodes 
    (println (new-detected-nodes 
        {:node [
           {:nodeId 0 :nodeClass 2 
            :dataType 
                [{
                    :dataId 5 
                    :measureStrategy 1
                    :type 1
                    :range [0 50]
                    :unit "C"
                    :dataCategory 1
                }] 
            }
        ] } 0 0)))

