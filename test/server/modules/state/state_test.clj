(ns server.modules.state.state-test
  (:require [clojure.test :refer :all]
            [server.modules.state.state :as state]
            [server.db :as db]
            [server.notification :as notification]
            (monger [db :as md] [collection :as mc])))

(deftest test-app

  (md/drop-db db/db)

  (testing "inserting new data ok"
    (let [obj {:data [
                  {:nodeId 3 
                    :dataId 1 
                    :value 13
                    :timestamp 1471531800}]} houseId 1 controllerId 1]
      (is (= (state/new-data obj houseId controllerId) {:status 200}))))

  (testing "inserting new data without houseId"
    (let [obj {:data [
                  {:nodeId 1 
                    :dataId 1 
                    :value 20 
                    :timestamp 1471531800}]} houseId nil controllerId 1]
      (is (= (state/new-data obj houseId nil) {:status 400 :errorMessage "houseId not defined"}))))

  (testing "inserting new data without data"
    (let [obj {} houseId 1]
      (is (= (state/new-data obj houseId nil) {:status 400 :errorMessage "Data not defined"}))))  

  (testing "inserting new data without nodeId"
    (let [obj {:data [
                  {:dataId 1 
                    :value 20 
                    :timestamp 1471531800}]} houseId 1]
      (is (= (state/new-data obj houseId nil) {:status 400 :errorMessage "Define nodeId, dataId, value and timestamp."}))))





  

  (testing "inserting new command ok"
    (let [obj {:command [
                  {:nodeId 3 
                    :commandId 1 
                    :value 20 
                    :timestamp 1471531800}]} houseId 1 controllerId 1]
      (is (= (state/new-command obj houseId controllerId) {:status 200}))))

  (testing "inserting new command without houseId"
    (let [obj {:command [
                  {:nodeId 1 
                    :commandId 1 
                    :value 20 
                    :timestamp 1471531800}]} houseId nil]
      (is (= (state/new-command obj houseId nil) {:status 400 :errorMessage "houseId not defined"}))))

  (testing "inserting new command without command"
    (let [obj {} houseId 1]
      (is (= (state/new-command obj houseId nil) {:status 400 :errorMessage "Command not defined"}))))  

  (testing "inserting new command without nodeId"
    (let [obj {:command [
                  {:commandId 1 
                    :value 20 
                    :timestamp 1471531800}]} houseId 1]
      (is (= (state/new-command obj houseId nil) {:status 400 :errorMessage "Define nodeId, commandId, value and timestamp."}))))







  (testing "inserting new action ok"
    (let [houseId 1 agentId 4 
          obj {:action {:controllerId 4
                        :nodeId 3 
                        :commandId 1 
                        :value 20}}]
      (is (= (state/new-action obj houseId agentId) {:status 200}))))

  (testing "inserting new action without houseId"
    (let [obj {:action {:nodeId 1 
                    :controllerId 1
                    :commandId 1 
                    :value 20 
                    :timestamp 1471531800}} houseId nil]
      (is (= (state/new-action obj houseId nil) {:status 400 :errorMessage "houseId not defined"}))))

  (testing "inserting new action without action"
    (let [obj {} houseId 1]
      (is (= (state/new-action obj houseId nil) {:status 400 :errorMessage "Action not defined"}))))  

  (testing "inserting new action without nodeId"
    (let [obj {:action [
                  {:commandId 1 
                    :controllerId 1
                    :value 20 
                    :timestamp 1471531800}]} houseId 1]
      (is (= (state/new-action obj houseId nil) {:status 400 :errorMessage "Define nodeId, commandId and value."}))))




  (testing "inserting multiples"
    (let [houseId 1 controllerId 1 obj {:data [
                            {:nodeId 1 :dataId 1 :value 21 :timestamp 1471531800},
                            {:nodeId 2 :dataId 1 :value 200 :timestamp 1471531801},
                            {:nodeId 1 :dataId 2 :value 22 :timestamp 1471531802},
                            {:nodeId 2 :dataId 2 :value 201 :timestamp 1471531803}
                            ]}]
      (is (= (state/new-data obj houseId controllerId) {:status 200}))))

  (testing "get house state"
    (let [houseId 1 obj {:state [{:command {:1 20}, :controllerId 1, :data {:1 13}, :nodeId 3}
                   {:command {:1 20}, :controllerId 4, :nodeId 3}
                   {:controllerId 1, :data {:1 21, :2 22}, :nodeId 1}
                   {:controllerId 1, :data {:1 200, :2 201}, :nodeId 2}],
           :status 200}]
    (is (= (state/get-house-state nil houseId nil) obj))))






  (testing "mock request from controller to server"
    (let [houseId 1
          obj {:result 1 :action {:nodeId 1 :commandId 1 :value 1}}]
      (is (= 200 (:status (state/send-action-result obj houseId nil))))))






  #_(testing "sending request to FCM server."
    (let [token1 "eEHWFv7EdA0:APA91bGO8WmaMpionMdkoOQ9LLouVaL7K3E9WhN6ztRIha2Xcl1vDfTokQotTeHr3QzimryG5dUwlu02xdkb2YbeK0eTal5cGfkca4CC1lePsOkMqR71W-9dkm47jAfKQwhOHnZejTT1"
          token2 "cpHCmaffX0Q:APA91bEIEd4L7vBTMm5D4nT2V7sidA519z5LqplzIlxrG0Et_UYXXwu0rFg3bQJ412Hrcuqwk4SbtmTywC7IpCYfxyLdBA8BpTWyuRB3B7deWJv8jYYNd6_Zjhgjth2qIeFQQeSJ5j1r"
          houseId 1
          tokens {1 #{token1} 2 #{token2}}
          response (state/notify-action-result houseId tokens "New action")]
      (is true? response)
      (doall (map #(is (= (:status %) 200)) @notification/thread-pool))
      (doall (map #(is (= ((read-string (apply str (filter (complement #{\:}) (:body %)))) "success") 1)) @notification/thread-pool))
    )
  )
)