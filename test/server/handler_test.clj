(ns server.handler-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [clojure.data.json :as json]
            [server.handler :refer :all]))

(deftest test-app

    (testing "basic route test"
        (let [response (app (mock/request :get "/"))]
            (is (= (:status response) 200))
            (is (= (:body response) "Server running"))
        )
    )

    (testing "basic route test"
        (let
            [
                response (app (assoc (mock/request :post "/")
                    :params {"payload" (json/write-str {"function" "testBase"})}))
                response-body (json/read-str (:body response) :key-fn keyword)
            ]
            (is (= (:status response) 200))
            (is (= (:status response-body) 200))
            (is (empty? (:errorMessage response-body)))
        )
    )

    (testing "not-found route"
        (let [response (app (mock/request :get "/invalid"))]
            (is (= (:status response) 404))
        )
    )
)
