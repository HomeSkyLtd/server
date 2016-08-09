(ns server.handler-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [server.handler :refer :all]))

(defn- filter-cookie [set-cookie]
  (apply str (take-while (complement #{\;}) (first set-cookie))))

(deftest test-app
  (testing "check route"
    (let [response (app (mock/request :get "/check"))]
      (is (= (:status response) 403))
      (is (= (:body response) "<h1>FORBIDDEN</h1>"))))

  (testing "main route"
    (let [response (app (mock/request :get "/"))]
      (def cookie (filter-cookie ((:headers response) "Set-Cookie")))
      (is (= (:status response) 200))
      (is (= (:body response) "<h1>Hello, Stranger!</h1>"))))

  (testing "check route wth cookie"
    (let [response (app 
                      (assoc 
                        (mock/request :get "/check") 
                        :headers {"cookie" cookie}))]
      (is (= (:status response) 200))
      (is (= (:body response) "<h3>Session on</h3>"))))

  (testing "insertion in db"
    (let [response (app 
                      (assoc 
                        (mock/request :post "/post")
                        :params {"fname" "John", "lname" "von Neumann", "collection" "testTable"}))]
      (is (= (:status response) 200))))

  (testing "selecting from db"
    (let [response (app 
                      (assoc 
                        (mock/request :post "/find")
                        :params {"key" "fname", "value" "John", "collection" "testTable"}))]
      (is (= (:status response) 200))
      (is (= (:body response)
              "{:lname \"von Neumann\", :fname \"John\"}"))))

  (testing "not-found route"
    (let [response (app (mock/request :get "/invalid"))]
      (is (= (:status response) 404)))))
