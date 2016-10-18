(ns server.modules.node.node-test
  (:use [clojure.test])
  (:use [server.modules.node.node])
  (:require [server.db :as db]
    [server.notification :as notification]
    (monger [db :as md]))
  )

;
; Check if all function exists
;

;
; Clear database
;

(defn create-destroy-coll [f]
    (db/remove "node_1")
    (f)
    )

(use-fixtures :once create-destroy-coll)

;;
;;  Initialize tokens in Notification
;;
(notification/init-tokens)


; All test functions use house-id 0

(deftest test-new-detected-nodes 
    (let [houseId 1]
        (testing "Valid nodes"
            (is (= {:status 200}
                    (new-detected-nodes 
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
                            ]
                        }
                        houseId 
                        0
                    )
                )
            )
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
                    ]} houseId 1)
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
                    ]} houseId 0)
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
                    ]} houseId 0)
            ) 400))
            (is (= (:status (new-detected-nodes 
                { } houseId 0)
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
                    ]} houseId 0)
            ) 400)))))



(deftest test-accept-node
    (let [houseId 1]
        (testing "accepting existing" 
            (is (= (:status (accept-node
                {:nodeId 123 :controllerId 0 :accept 1} houseId 5)
            ) 200)))
        (testing "reject existing" 
            (is (= (:status (accept-node
                {:nodeId 123 :controllerId 1 :accept 0} houseId 5)
            ) 200)))
        (testing "accept or reject non existing"
            (is (= (:status (accept-node
                {:nodeId 1243 :controllerId 1 :accept 1} houseId 5)
            ) 400))
            (is (= (:status (accept-node
                {:nodeId 123 :controllerId 1 :accept 0} houseId 5)
            ) 400)))
        (testing "accept or reject accepted"
            (is (= (:status (accept-node
                {:nodeId 123 :controllerId 0 :accept 1} houseId 5)
            ) 400))
            (is (= (:status (accept-node
                {:nodeId 123 :controllerId 0 :accept 0} houseId 5)
            ) 400)))
        )
    )


(deftest test-set-node-extra
    (let [houseId 1]
        (testing "set extra of existing node" 
            (is (= (:status (set-node-extra
                {:nodeId 123 :controllerId 0 :extra {:name "Presence sensor" :t "1231"}} houseId 5)
            ) 200))
            (is (= (:status (set-node-extra
                {:nodeId 123 :controllerId 0 :extra {:name "Test" :room "Living room"}} houseId 5)
            ) 200)))
        (testing "set extra of non existing node" 
            (is (= (:status (set-node-extra
                {:nodeId 125 :controllerId 0 :extra {:name "Presence sensor" :t "1231"}} houseId 5)
            ) 400)))))


(deftest test-get-nodes-info
    (testing "get nodes info"
        (let [houseId 1 response (get-nodes-info 0 houseId 0)]
            (and (is (= (:status response) 200))
                (is (not (empty? (:nodes response))))))))


(deftest test-set-node-state
    (let [houseId 1]
        (testing "set state of existing node" 
            (is (= (:status (set-node-state
                {:nodeId 123 :alive 0 } houseId 0)
            ) 200))
            (is (= (:alive (first (:nodes (get-nodes-info 0 houseId 0)))) 0))
            (is (= (:status (set-node-state
                {:nodeId 123 :alive 1 } houseId 0)
            ) 200))
            (is (= (:alive (first (:nodes (get-nodes-info 0 houseId 0)))) 1)))
        
        (testing "set state of non existing node" 
            (is (= (:status (set-node-state
                {:nodeId 129 :alive 0 } houseId 0)
            ) 400)))))


(deftest test-remove-node
    (let [houseId 1]
        (testing "remove non accepted node" 
            (is (= (:status (remove-node
                {:nodeId 126 :controllerId 0 } houseId 5)
            ) 400)))
        (testing "remove non existing node" 
            (is (= (:status (remove-node
                {:nodeId 125 :controllerId 0 } houseId 5)
            ) 400)))
        (testing "remove accepted node" 
            (accept-node {:nodeId 126 :controllerId 0 :accept 1} houseId 5)
            (is (= (:status (remove-node
                {:nodeId 126 :controllerId 0 } houseId 5)
            ) 200))))
        ;Wait for other threads to finish (gambeta)
        (Thread/sleep 10000)
    )