(ns server.modules.rule.rule-test
  (:require [clojure.test :refer :all]
            [server.modules.rule.rule :as rule]
            [server.db :as db]
            [server.notification :as notification]
            (monger [db :as md] [collection :as mc])))

(def coll-name "rules_1")

(deftest test-app

  ;;
  ;;  Initialize tokens in Notification
  ;;
  (notification/init-tokens)

  (md/drop-db db/db)
  (mc/insert db/db coll-name {:accepted 0 :controllerId 1 :command {:nodeId 2 :commandId 1 :value 1} :clauses [{:lhs "2.1", :operator ">", :rhs "0"}]})

  (testing "new rule ok"
    (let [obj {:rules [{:command {
                          :nodeId 1 
                          :commandId 1 
                          :value 20 
                        }
                        :controllerId 1
                        :clauses [{:lhs "1.1", :operator ">", :rhs "20"}]}]} 
          houseId 1]
      (is (= 200 (:status (rule/new-rules obj houseId nil))))))

  (testing "new rule without rules"
    (let [obj nil houseId 1]
      (is (= 400 (:status (rule/new-rules obj houseId nil))))))

 
  (testing "new rule without nodeId"
    (let [obj {:rules [{:controllerId 1 :commandId 1 :value 20 :clauses [{:lhs "1.1", :operator ">", :rhs "20"}]}]}
            houseId 1]
      (is (= 400 (:status (rule/new-rules obj houseId nil))))))

  (testing "new rule without controllerId"
    (let [obj {:rules [{:nodeId 1 :commandId 1 :value 20 :clauses [{:lhs "1.1", :operator ">", :rhs "20"}]}]}
            houseId 1]
      (is (= 400 (:status (rule/new-rules obj houseId nil))))))

  (testing "new rule without commandId"
    (let [obj {:rules [{:controllerId 1 :nodeId 1 :value 20 :clauses [{:lhs "1.1", :operator ">", :rhs "20"}]}]}
            houseId 1]
      (is (= 400 (:status (rule/new-rules obj houseId nil))))))






  (testing "select from db"
    (let [houseId 1 obj {:status 200 
                         :rules [{:command {
                                    :nodeId 1
                                    :commandId 1 
                                    :value 20
                                  }
                                 :controllerId 1
                                 :clauses [{:lhs "1.1", :operator ">", :rhs "20"}]}]}]
      (is (= (rule/get-rules {} houseId nil) obj))))






  (testing "select learnt rules from db"
    (let [houseId 1 obj {:status 200 
                         :rules [{:command {
                                    :nodeId 2
                                    :commandId 1
                                    :value 1
                                   }
                                  :controllerId 1
                                  :clauses [{:lhs "2.1", :operator ">", :rhs "0"}]
                                  }
                                ]
                        }]
      (is (= (rule/get-learnt-rules {} houseId nil) obj))))





    (testing "accepting rule"
      (let [houseId 1 
            obj {:controllerId 1 :command {:nodeId 2 :commandId 1 :value 1}}
            result (rule/accept-rule obj houseId nil)]
        (is (and
              (= 200 (:status result))
              (not (contains? result :conflictingRule))))
        (is (= 1 (:accepted (mc/find-one-as-map db/db coll-name obj))))))



;TODO: Test a conflict rule



    (testing "remove rule"
      (let [houseId 1
            obj {:controllerId 1 :command {:nodeId 2 :commandId 1 :value 1}}]
            (is (= {:status 200} (rule/remove-rule obj houseId nil)))
            (is (empty? (db/select coll-name obj)))))

    (testing "try to remove rule that is not in DB"
      (let [houseId 1
            obj {:controllerId 123 :command {:nodeId 123 :commandId 123 :value 123}}]
            (is (= {:status 400 :errorMessage "DB does not contain obj."} (rule/remove-rule obj houseId nil)))
            (is (empty? (db/select coll-name obj)))))

    )