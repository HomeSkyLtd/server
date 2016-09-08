(ns server.modules.node.node-test
  (:use [clojure.test])
  (:use [server.modules.node.node])
  (:require [server.db :as db]
            [server.notification :as notification])
  )

;
; Check if all function exists
;

;
; Clear database
;

(defn create-destroy-coll [f]
    (db/remove "node_0")
    (f)
    )

(use-fixtures :once create-destroy-coll)


; All test functions use house-id 0

(deftest test-new-detected-nodes 
    (testing "Valid nodes"
        (is (= (:status (new-detected-nodes 
            {:node [ 
               {:nodeId 123 :nodeClass 2 
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
            ]} 0 0)
        ) 200))
        (is (= (:status (new-detected-nodes 
            {:node [ 
               {:nodeId 123 :nodeClass 2 
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
            ]} 0 1)
        ) 200))
         (is (= (:status (new-detected-nodes 
            {:node [ 
               {:nodeId 126 :nodeClass 2 
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
            ]} 0 0)
        ) 200))
    )
    (testing "Invalid nodes"
        (is (= (:status (new-detected-nodes 
            {:node [ 
               { :nodeClass 2 
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
            ]} 0 0)
        ) 400))
        (is (= (:status (new-detected-nodes 
            { } 0 0)
        ) 400))
    )
    (testing "Existent node"
        (is (= (:status (new-detected-nodes 
            {:node [ 
               {:nodeId 123 :nodeClass 2 
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
            ]} 0 0)
        ) 400))))

(deftest test-accept-node
    (testing "accepting existing" 
        (is (= (:status (accept-node
            {:nodeId 123 :controllerId 0 :accept 1} 0 5)
        ) 200)))
    (testing "reject existing" 
        (is (= (:status (accept-node
            {:nodeId 123 :controllerId 1 :accept 0} 0 5)
        ) 200)))
    (testing "accept or reject non existing"
        (is (= (:status (accept-node
            {:nodeId 1243 :controllerId 1 :accept 1} 0 5)
        ) 400))
        (is (= (:status (accept-node
            {:nodeId 123 :controllerId 1 :accept 0} 0 5)
        ) 400)))
    (testing "accept or reject accepted"
        (is (= (:status (accept-node
            {:nodeId 123 :controllerId 0 :accept 1} 0 5)
        ) 400))
        (is (= (:status (accept-node
            {:nodeId 123 :controllerId 0 :accept 0} 0 5)
        ) 400)))
)


(deftest test-set-node-extra
    (testing "set extra of existing node" 
        (is (= (:status (set-node-extra
            {:nodeId 123 :controllerId 0 :extra {:name "Presence sensor" :t "1231"}} 0 5)
        ) 200))
        (is (= (:status (set-node-extra
            {:nodeId 123 :controllerId 0 :extra {:name "Test" :room "Living room"}} 0 5)
        ) 200)))
    (testing "set extra of non existing node" 
        (is (= (:status (set-node-extra
            {:nodeId 125 :controllerId 0 :extra {:name "Presence sensor" :t "1231"}} 0 5)
        ) 400))))


(deftest test-get-nodes-info
    (testing "get nodes info"
        (let [response (get-nodes-info 0 0 0)]
            (and (is (= (:status response) 200))
                (is (not (empty? (:nodes response))))))))


(deftest test-set-node-state
    (testing "set state of existing node" 
        (is (= (:status (set-node-state
            {:nodeId 123 :alive 0 } 0 0)
        ) 200))
        (is (= (:alive (first (:nodes (get-nodes-info 0 0 0)))) 0))
        (is (= (:status (set-node-state
            {:nodeId 123 :alive 1 } 0 0)
        ) 200))
        (is (= (:alive (first (:nodes (get-nodes-info 0 0 0)))) 1)))
    
    (testing "set state of non existing node" 
        (is (= (:status (set-node-state
            {:nodeId 129 :alive 0 } 0 0)
        ) 400))))


(deftest test-remove-node
    (testing "remove non accepted node" 
        (is (= (:status (remove-node
            {:nodeId 126 :controllerId 0 } 0 5)
        ) 400)))
    (testing "remove non existing node" 
        (is (= (:status (remove-node
            {:nodeId 125 :controllerId 0 } 0 5)
        ) 400)))
    (testing "remove accepted node" 
        (accept-node {:nodeId 126 :controllerId 0 :accept 1} 0 5)
        (is (= (:status (remove-node
            {:nodeId 126 :controllerId 0 } 0 5)
        ) 200))))


(deftest notification
    (testing "sending request to FCM server."
        (let [token1 "eEHWFv7EdA0:APA91bGO8WmaMpionMdkoOQ9LLouVaL7K3E9WhN6ztRIha2Xcl1vDfTokQotTeHr3QzimryG5dUwlu02xdkb2YbeK0eTal5cGfkca4CC1lePsOkMqR71W-9dkm47jAfKQwhOHnZejTT1"
              token2 "cpHCmaffX0Q:APA91bEIEd4L7vBTMm5D4nT2V7sidA519z5LqplzIlxrG0Et_UYXXwu0rFg3bQJ412Hrcuqwk4SbtmTywC7IpCYfxyLdBA8BpTWyuRB3B7deWJv8jYYNd6_Zjhgjth2qIeFQQeSJ5j1r"
              houseId 1
              tokens {houseId #{token1 token2}}
              response (notify-detected-nodes houseId tokens "New nodes")]
        (is true? response)
        (doall (map #(is (= (:status %) 200)) @notification/thread-pool))
        (doall (map #(is (= ((read-string (apply str (filter (complement #{\:}) (:body %)))) "success") 1)) @notification/thread-pool)))))