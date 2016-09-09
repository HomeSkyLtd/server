(ns server.notification-test
  (:require [clojure.test :refer :all]
  			[server.notification :as notification]))

(deftest test-notification-controller

  (def token1 "eEHWFv7EdA0:APA91bGO8WmaMpionMdkoOQ9LLouVaL7K3E9WhN6ztRIha2Xcl1vDfTokQotTeHr3QzimryG5dUwlu02xdkb2YbeK0eTal5cGfkca4CC1lePsOkMqR71W-9dkm47jAfKQwhOHnZejTT1")
  (def token2 "cpHCmaffX0Q:APA91bEIEd4L7vBTMm5D4nT2V7sidA519z5LqplzIlxrG0Et_UYXXwu0rFg3bQJ412Hrcuqwk4SbtmTywC7IpCYfxyLdBA8BpTWyuRB3B7deWJv8jYYNd6_Zjhgjth2qIeFQQeSJ5j1r")
  (def tokens {1 #{token1}}); token2}})
  (swap! notification/tokens merge tokens)


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
        (let [token1 "eEHWFv7EdA0:APA91bGO8WmaMpionMdkoOQ9LLouVaL7K3E9WhN6ztRIha2Xcl1vDfTokQotTeHr3QzimryG5dUwlu02xdkb2YbeK0eTal5cGfkca4CC1lePsOkMqR71W-9dkm47jAfKQwhOHnZejTT1"
              token2 "cpHCmaffX0Q:APA91bEIEd4L7vBTMm5D4nT2V7sidA519z5LqplzIlxrG0Et_UYXXwu0rFg3bQJ412Hrcuqwk4SbtmTywC7IpCYfxyLdBA8BpTWyuRB3B7deWJv8jYYNd6_Zjhgjth2qIeFQQeSJ5j1r"
              houseId 1
              tokens {houseId #{token1}}; token2}}
              nodes [{:nodeId 1} {:nodeId 2} {:nodeId 3}]]
        (swap! notification/tokens merge tokens)
        (is true? (notification/notify-detected-nodes houseId nodes))
        (doall (map #(is (= (:status %) 200)) @notification/thread-pool))
        (doall (map #(is (= ((read-string (apply str (filter (complement #{\:}) (:body %)))) "success") 1)) @notification/thread-pool)))))