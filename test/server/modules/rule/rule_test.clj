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
  (mc/insert db/db coll-name {:accepted 0 :nodeId 2 :controllerId 1 :commandId 1 :value 1 :clauses [{:lhs "2.1", :operator ">", :rhs "0"}]})

  (testing "new node ok"
    (let [obj {:rules [{:nodeId 1 
                        :controllerId 1
                        :commandId 1 
                        :value 20 
                        :clauses [{:lhs "1.1", :operator ">", :rhs "20"}]}]} 
          houseId 1]
      (is (= 200 (:status (rule/new-rules obj houseId nil))))))

  (testing "new node without rules"
    (let [obj nil houseId 1]
      (is (= 400 (:status (rule/new-rules obj houseId nil))))))

 
  (testing "new node without nodeId"
    (let [obj {:rules [{:controllerId 1 :commandId 1 :value 20 :clauses [{:lhs "1.1", :operator ">", :rhs "20"}]}]}
            houseId 1]
      (is (= 400 (:status (rule/new-rules obj houseId nil))))))

  (testing "new node without controllerId"
    (let [obj {:rules [{:nodeId 1 :commandId 1 :value 20 :clauses [{:lhs "1.1", :operator ">", :rhs "20"}]}]}
            houseId 1]
      (is (= 400 (:status (rule/new-rules obj houseId nil))))))

  (testing "new node without commandId"
    (let [obj {:rules [{:controllerId 1 :nodeId 1 :value 20 :clauses [{:lhs "1.1", :operator ">", :rhs "20"}]}]}
            houseId 1]
      (is (= 400 (:status (rule/new-rules obj houseId nil))))))






  (testing "select filterom db"
    (let [houseId 1 obj {:status 200 
                         :rules [{:accepted 1
                                 :nodeId 1
                                 :controllerId 1
                                 :commandId 1 
                                 :value 20
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
      (let [houseId 1 obj {:nodeId 2 :controllerId 1 :commandId 1 :value 1}]
        (rule/accept-rule obj houseId nil)
        (is (= (dissoc (mc/find-one-as-map db/db coll-name obj) :_id) (assoc obj :accepted 1 :clauses [{:lhs "2.1", :operator ">", :rhs "0"}])))))






    (testing "remove rule"
      (let [houseId 1
            obj {:nodeId 2 :controllerId 1 :commandId 1 :value 1}]
            (is (= 200 (:status (rule/remove-rule obj houseId nil))))
            (is (empty? (db/select coll-name obj)))))

    (testing "try to remove rule that is not in DB"
      (let [houseId 1
            obj {:nodeId 3 :controllerId 1 :commandId 1 :value 1}]
            (is (= (rule/remove-rule obj houseId nil) {:status 400 :errorMessage "DB does not contain obj."}))
            (is (empty? (db/select coll-name obj)))))
    

    )