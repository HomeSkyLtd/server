(ns server.modules.rule.rule-test
  (:require [clojure.test :refer :all]
            [server.modules.rule.rule :as rule]
            [server.db :as db]
            (monger [db :as md] [collection :as mc])))

(deftest test-app

  (md/drop-db db/db)
  (mc/insert db/db "rules_1" {:agentId 1 :accepted false :nodeId 2 :controllerId 1 :commandId 1 :value 1 :clauses [{:lhs "2.1", :operator ">", :rhs "0"}]})

  (testing "new node ok"
    (let [obj {:rules [
                  {:nodeId 1 
                    :controllerId 1
                    :commandId 1 
                    :value 20 
                    :clauses [{:lhs "1.1", :operator ">", :rhs "20"}]}]} houseId 1 agentId 1]
      (is (= (rule/new-rules obj houseId agentId) {:status 200}))))

  (testing "new node without rules"
    (let [obj nil houseId 1 agentId 1]
      (is (= (rule/new-rules obj houseId agentId) {:status 400 :errorMessage "Rules not defined"}))))

 
  (testing "new node without nodeId"
    (let [obj {:rules [{:controllerId 1 :commandId 1 :value 20 :clauses [{:lhs "1.1", :operator ">", :rhs "20"}]}]}
            houseId 1 agentId 1]
      (is (= (rule/new-rules obj houseId agentId) 
                {:status 400 :errorMessage "Define nodeId, controllerId, commandId, value and clauses."}))))

  (testing "new node without controllerId"
    (let [obj {:rules [{:nodeId 1 :commandId 1 :value 20 :clauses [{:lhs "1.1", :operator ">", :rhs "20"}]}]}
            houseId 1 agentId 1]
      (is (= (rule/new-rules obj houseId agentId) 
                {:status 400 :errorMessage "Define nodeId, controllerId, commandId, value and clauses."}))))

  (testing "new node without commandId"
    (let [obj {:rules [{:controllerId 1 :nodeId 1 :value 20 :clauses [{:lhs "1.1", :operator ">", :rhs "20"}]}]}
            houseId 1 agentId 1]
      (is (= (rule/new-rules obj houseId agentId) 
                {:status 400 :errorMessage "Define nodeId, controllerId, commandId, value and clauses."}))))


  (testing "select from db"
    (let [houseId 1 agentId 1 obj {:accepted true
                          :nodeId 1
                          :controllerId 1
                          :agentId 1
                          :commandId 1 
                          :value 20
                          :clauses [{:lhs "1.1", :operator ">", :rhs "20"}]}]
      (is (= (dissoc (first (rule/get-rules {} houseId agentId)) :_id) obj))))


  (testing "select learnt rules from db"
    (let [houseId 1 agentId 1 obj {:accepted false 
                          :nodeId 2 
                          :controllerId 1
                          :agentId 1
                          :commandId 1 
                          :value 1 
                          :clauses [{:lhs "2.1", :operator ">", :rhs "0"}]}]
      (is (= (dissoc (first (rule/get-learnt-rules {} houseId agentId)) :_id) obj))))


  (testing "sending request to FCM server."
    (let [token "eEHWFv7EdA0:APA91bGO8WmaMpionMdkoOQ9LLouVaL7K3E9WhN6ztRIha2Xcl1vDfTokQotTeHr3QzimryG5dUwlu02xdkb2YbeK0eTal5cGfkca4CC1lePsOkMqR71W-9dkm47jAfKQwhOHnZejTT1"
          response (rule/notify-learnt-rules token "New rules")]
      (is (= (:status response) 200))
      (is (= ((read-string (apply str (filter (complement #{\:}) (:body response)))) "success") 1))
    )))
