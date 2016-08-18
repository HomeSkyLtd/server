(ns server.modules.auth.auth-test
  (:use [clojure.test])
  (:use [server.modules.node.node])
  (:require
      [server.db :as db]
      [server.handler :as handler :only [app]]
      [ring.mock.request :as mock]
      [clojure.data.json :as json]
      (monger [core :as mg] [collection :as mc] [result :as res] [db :as md]))
)

(deftest test-app
    (md/drop-db db/db)
    (testing "unauthorized - not logged in"
        (let [response-body (json/read-str (:body (handler/app (assoc (mock/request :post "/")
            :params {"payload" (json/write-str {"function" "test"})})
            )) :key-fn keyword)]
            (is (= (:status response-body) 403))
            (is (not (empty? (:errorMessage response-body))))
        )
    )
    (testing "creating admin user"
        (let [
                response (handler/app (assoc (mock/request :post "/")
                    :params {"payload" (json/write-str {
                        "function" "newAdmin",
                        "username" "admin1",
                        "password" "mypass"})}))
                response-body (json/read-str (:body response) :key-fn keyword)
                inserted-admin (first (db/select "agent" {"username" "admin1"}))
                inserted-house (first (db/select "house" {}))
            ]
            (is (not (nil? inserted-admin)))
            (is (not (nil? inserted-house)))
            (is (= (:password inserted-admin) "mypass"))
            (is (= (:houseId inserted-admin) (str (:_id inserted-house))))
            (is (= (:status response-body) 200))
            (is (empty? (:errorMessage response-body)))
        )
    )
    (testing "creating admin user with invalid credentials"
        (let [
                response-body (json/read-str (:body (handler/app (assoc (mock/request :post "/")
                    :params {"payload" (json/write-str {
                        "function" "newAdmin",
                        "username" "",
                        "password" "mypass"})}))) :key-fn keyword)
            ]
            (println response-body)
            (is (= (:status response-body) 400))
            (is (not (empty? (:errorMessage response-body))))
        )
    )
    (testing "logging in"
        (let [
                response (handler/app (assoc (mock/request :post "/")
                    :params {"payload" (json/write-str {
                        "function" "login",
                        "username" "admin1",
                        "password" "mypass"})}))
            ]
            (println response)
            (is (= 1 1))
        )
    )
)
