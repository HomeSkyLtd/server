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

  (testing "not-found route"
    (let [response (app (mock/request :get "/invalid"))]
      (is (= (:status response) 404)))))
