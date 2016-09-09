(ns server.notification-test
  (:require [clojure.test :refer :all]
  			[server.notification :as notification]))

(deftest test-notification-controller

	(testing "auxiliar functions to count number of new rules"
      (let [rules 
              [{:controllerId 1
                :nodeId 1
                :commandId 1
                :value 1
                :clauses [{:lhs "1.1" :operator "==" :rhs 0}]
              },
              {
                :controllerId 2
                :nodeId 1
                :commandId 1
                :value 1
                :clauses [{:lhs "1.1" :operator "==" :rhs 0}]
              },
              {
                :controllerId 1
                :nodeId 2
                :commandId 1
                :value 1
                :clauses [{:lhs "1.1" :operator "==" :rhs 0}]
              },
              {
                :controllerId 3
                :nodeId 1
                :commandId 1
                :value 1
                :clauses [{:lhs "1.1" :operator "==" :rhs 0}]
              }]
            ]

          (is (= '(1 2 3) (notification/list-controller-ids rules)))
          (is (= {:notification "newRules" :quantity 2} (notification/build-count rules 1)))
          (is (= {:notification "newRules" :quantity 1} (notification/build-count rules 2)))
          (is (= {:notification "newRules" :quantity 1} (notification/build-count rules 3))))))

(deftest test-notification-app
    (testing "sending request to FCM server."
        (let [houseId 1
              nodes [{:nodeId 1} {:nodeId 2} {:nodeId 3}]]
        (is (= 200 (:status (notification/notify-detected-nodes houseId nodes)))))))
    