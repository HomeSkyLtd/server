(ns server.modules.rule.rule-test
  (:require [clojure.test :refer :all]
            [server.modules.rule.rule :as rule]))

(deftest test-app
  (testing "new node ok"
    (let [obj {:rules [{:command {:nodeId 1 :commandId 1 :value 20} :clauses [{:lhs "1.1", :operator ">", :rhs "20"}]}]}
          houseId 1]
      (is (= (first (rule/new-rules obj houseId)) {:status 200}))))

  (comment (testing "new node without rules"
    (let [obj nil houseId 1]
      (is (= (rule/new-rules obj houseId) {:status 400 :errorMessage "Rules not defined"}))))

  (testing "new node without houseId"
    (let [obj {:rules [{:command {:nodeId 1 :commandId 1 :value 20} :clauses [{:lhs "1.1", :operator ">", :rhs "20"}]}]}
          houseId nil]
      (is (= (rule/new-rules obj houseId) {:status 400 :errorMessage "houseId not defined"}))))

  (testing "new node without nodeId"
    (let [obj {:rules [{:command {:commandId 1 :value 20} :clauses [{:lhs "1.1", :operator ">", :rhs "20"}]}]}
            houseId 1]
      (is (= (first (rule/new-rules obj houseId)) {:status 400 :errorMessage "Define nodeId, commandId, value and clauses."}))))

  (testing "new node without commandId"
    (let [obj {:rules [{:command {:nodeId 1 :value 20} :clauses [{:lhs "1.1", :operator ">", :rhs "20"}]}]}
            houseId 1]
      (is (= (first (rule/new-rules obj houseId)) {:status 400 :errorMessage "Define nodeId, commandId, value and clauses."}))))

  (testing "new node without value"
    (let [obj {:rules [{:command {:nodeId 1 :commandId 1} :clauses [{:lhs "1.1", :operator ">", :rhs "20"}]}]}
            houseId 1]
      (is (= (first (rule/new-rules obj houseId)) {:status 400 :errorMessage "Define nodeId, commandId, value and clauses."}))))

  (testing "new node without clauses"
    (let [obj {:rules [{:command {:nodeId 1 :commandId 1 :value 20}}]}
            houseId 1]
      (is (= (first (rule/new-rules obj houseId)) {:status 400 :errorMessage "Define nodeId, commandId, value and clauses."}))))))