(ns server.modules.rule.rule-test
  (:require [clojure.test :refer :all]
            [server.modules.rule.rule :as rule]
            [server.db :as db]
            (monger [db :as md] [collection :as mc])))

(deftest test-app

  (md/drop-db db/db)
  (mc/insert db/db "rules_1" {:accepted false :nodeId 2 :commandId 1 :value 1 :clauses [{:lhs "2.1", :operator ">", :rhs "0"}]})

  (testing "new node ok"
    (let [obj {:rules [
                  {:nodeId 1 
                    :commandId 1 
                    :value 20 
                    :clauses [{:lhs "1.1", :operator ">", :rhs "20"}]}]} houseId 1 agentId 1]
      (is (= (rule/new-rules obj houseId agentId) {:status 200}))))

  (testing "new node without rules"
    (let [obj nil houseId 1 agentId 1]
      (is (= (rule/new-rules obj houseId agentId) {:status 400 :errorMessage "Rules not defined"}))))

  (testing "new node without houseId"
    (let [obj {:rules [
                  {:nodeId 1 
                    :commandId 1 
                    :value 20 
                    :clauses [{:lhs "1.1", :operator ">", :rhs "20"}]}]} houseId nil agentId 1]
      (is (= (rule/new-rules obj houseId agentId) {:status 400 :errorMessage "houseId not defined"}))))

  (testing "new node without nodeId"
    (let [obj {:rules [{:commandId 1 :value 20 :clauses [{:lhs "1.1", :operator ">", :rhs "20"}]}]}
            houseId 1 agentId 1]
      (is (= (rule/new-rules obj houseId agentId) 
                {:status 400 :errorMessage "Define nodeId, commandId, value and clauses."}))))

  (testing "select from db"
    (let [houseId 1 obj {:accepted true
                          :nodeId 1
                          :commandId 1 
                          :value 20
                          :agentId 1
                          :clauses [{:lhs "1.1", :operator ">", :rhs "20"}]} agentId nil]
      (is (= (dissoc (first (rule/get-rules nil houseId agentId)) :_id) obj))))

  (testing "select learnt rules from db"
    (let [houseId 1 obj {:accepted false 
                          :nodeId 2 
                          :commandId 1 
                          :value 1 
                          :clauses [{:lhs "2.1", :operator ">", :rhs "0"}]} agentId nil]
      (is (= (dissoc (first (rule/get-learnt-rules nil houseId agentId)) :_id) obj)))))
