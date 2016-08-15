(ns server.handler-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [server.handler :refer :all]))

(defn- filter-cookie [set-cookie]
  (apply str (take-while (complement #{\;}) (first set-cookie))))

(testing "main route"
    (let [response (app (mock/request :get "/"))]
      (is (= (:status response) 200))
      (is (= (:body response) "Server running"))))

(deftest test-app
  (testing "check route"
    (let [response (app (mock/request :get "/check"))]
      (is (= (:status response) 403))
      (is (= (:body response) "<h1>FORBIDDEN</h1>"))))

  (testing "login"
    (let [response (app (mock/request :get "/login"))]
      (def cookie (filter-cookie ((:headers response) "Set-Cookie")))
      (is (= (:status response) 200))
      (is (= (:body response) "<h1>Hello, Stranger!</h1>"))))

  (testing "check route wth cookie"
    (let [response (app (assoc (mock/request :get "/check") 
                                :headers {"cookie" cookie}))]
      (is (= (:status response) 200))
      (is (= (:body response) "<h3>Session on</h3>"))))

  (testing "insert data in db"
    (let [response (app (assoc (mock/request :post "/")
                                :params {"data" "[{\"nodeId\" 2, \"dataId\" 2, \"value\" 200}, 
                                                  {\"nodeId\" 2, \"dataId\" 1, \"value\" 23}]",
                                          "controllerId" 1,
                                          "function" "insert"}))]
      (is (= (:status response) 200))))

  (testing "insert external command in db"
    (let [response (app (assoc (mock/request :post "/")
                                :params {"xcommand" "{\"nodeId\" 3, \"commandId\" 1, \"value\" 1}",
                                          "controllerId" 1,
                                          "function" "insert"}))]
      (is (= (:status response) 200))))

  (testing "insert new node in db"
    (let [response (app (assoc (mock/request :post "/")
                                :params 
                                {"description" "{\"id\": 2, 
                                                  \"nodeClass\": \"sensor\", 
                                                  \"dataType\": [{\"id\": 1,
                                                                \"type\": \"bool\",
                                                                \"range\": [0, 1],
                                                                \"measureStrategy\": \"event\",
                                                                \"dataCategory\": \"presence\",
                                                                \"unit\": \"\"}]}",
                                "controllerId" 1,
                                "function" "insert"}))]
      (is (= (:status response) 200))))

   (testing "select data from db"
    (let [response (app (assoc (mock/request :post "/")
                                :params {"nodeId" 2, 
                                          "function" "select",
                                          "target" "data",
                                          "controllerId" 1}))]
      (is (= (:status response) 200))
      (is (= (:body response) "{:value 200, :dataId 2, :nodeId 2}{:value 23, :dataId 1, :nodeId 2}"))))

   (testing "select node from db"
    (let [response (app (assoc (mock/request :post "/")
                                :params {"nodeId" 2, 
                                          "function" "select",
                                          "target" "node",
                                          "controllerId" 1}))]
      (is (= (:status response) 200))
      (is (= (:body response) "{:dataType [{:unit \"\", :dataCategory \"presence\", :measureStrategy \"event\", :range [0 1], :type \"bool\", :id 1}], :nodeClass \"sensor\", :id 2}"))))

  (testing "not-found route"
    (let [response (app (mock/request :get "/invalid"))]
      (is (= (:status response) 404)))))
