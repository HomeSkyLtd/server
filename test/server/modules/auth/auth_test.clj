(ns server.modules.auth.auth-test
  (:use [clojure.test])
  (:require
      [server.db :as db]
      [server.handler :as handler :only [app tokens]]
      [server.modules.auth.auth :as auth :only [get-agents]]
      [server.notification :as notification :only [tokens]]
      [ring.mock.request :as mock]
      [clojure.data.json :as json]
      [clojure.string :only [split join] :as str]
      (monger [core :as mg] [collection :as mc] [result :as res] [db :as md]))
  (:import org.bson.types.ObjectId)
)

(defn- setup-database-test-handler []
    (md/drop-db db/db)
    (let 
        [
            inserted 
                (db/insert "agent" {:username "controller1", :password "AYag$s+h8FdzfVnY=$TO2dl9of6ilh5KAdZ3h9cASn3Kk=",
                :type "controller", :houseId ""} :return-inserted true) ;hash = ctrlpass
        ]
        (def controller-id (str (:_id inserted)))
    )
)

(defn- setup-database-test-db []
    (md/drop-db db/db)
    (db/insert "agent" {:username "admin1", :password "adminpass1",
        :type "admin", :houseId "1"})
    (db/insert "agent" {:username "admin2", :password "adminpass2",
        :type "admin", :houseId "2"})
    (db/insert "agent" {:username "user1", :password "userpass1",
        :type "user", :houseId "1"})
    (db/insert "agent" {:username "user2", :password "userpass2",
        :type "user", :houseId "1"})
    (db/insert "agent" {:username "controller1", :password "ctrlpass1",
        :type "controller", :houseId "1"})
    (db/insert "agent" {:username "controller2", :password "ctrlpass2",
        :type "controller", :houseId "2"})
)

(defn- process-header [header-str]
    "
        Receives as input a header in string format, and returns key-value pairs
        representing its fields. For example:
        -Input: session=abc;httpOnly;max-age=30
        -Output: [[session abc] [httponly] [max-age 30]]
    "
    (map #(str/split % #"=") (str/split header-str #";"))
    )

(defn- check-body-ok [response-body]
    (is (= (:status response-body) 200))
    (is (empty? (:errorMessage response-body)))
)

(defn- check-body-error [response-body status]
    (is (= (:status response-body) status))
    (is (not (empty? (:errorMessage response-body))))
)

(deftest test-handler-functions
    (setup-database-test-handler)
    (swap! notification/tokens {})

    (testing "unauthorized - not logged in"
        (let [response-body (json/read-str (:body (handler/app (assoc (mock/request :post "/")
            :params {"payload" (json/write-str {"function" "testAdmin"})})
            )) :key-fn keyword)]
            (check-body-error response-body 403)
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
            (def admin-house-id (keyword (:houseId inserted-admin)))

            (is (not (nil? inserted-admin)))
            (is (not (nil? inserted-house)))
            (is (= (:houseId inserted-admin) (str (:_id inserted-house))))
            (check-body-ok response-body)
        )
    )
    (testing "creating admin user with empty credentials"
        (let [
                response-body (json/read-str (:body (handler/app (assoc (mock/request :post "/")
                    :params {"payload" (json/write-str {
                        "function" "newAdmin",
                        "username" "",
                        "password" "mypass"})}))) :key-fn keyword)
            ]
            (check-body-error response-body 400)
        )
    )
    (testing "creating admin user with missing credentials"
        (let [
                response-body (json/read-str (:body (handler/app (assoc (mock/request :post "/")
                    :params {"payload" (json/write-str {
                        "function" "newAdmin",
                        "username" "admin1"})}))) :key-fn keyword)
            ]
            (check-body-error response-body 400)
        )
    )
    (testing "logging in with empty credentials"
        (let [
                response (handler/app (assoc (mock/request :post "/")
                    :params {"payload" (json/write-str {
                        "function" "login",
                        "username" "",
                        "password" "mypass"})}))
                response-body (json/read-str (:body response) :key-fn keyword)
                set-cookie-value (first ((:headers response) "Set-Cookie"))
            ]
            (is (nil? set-cookie-value))
            (check-body-error response-body 400)
        )
    )
    (testing "logging in with missing credentials"
        (let [
                response (handler/app (assoc (mock/request :post "/")
                    :params {"payload" (json/write-str {
                        "function" "login",
                        "username" "admin1"})}))
                response-body (json/read-str (:body response) :key-fn keyword)
                set-cookie-value (first ((:headers response) "Set-Cookie"))
            ]
            (is (nil? set-cookie-value))
            (check-body-error response-body 400)
        )
    )
    (testing "logging in with inexisting user"
        (let [
                response (handler/app (assoc (mock/request :post "/")
                    :params {"payload" (json/write-str {
                        "function" "login",
                        "username" "stranger",
                        "password" "mypass"})}))
                response-body (json/read-str (:body response) :key-fn keyword)
                set-cookie-value (first ((:headers response) "Set-Cookie"))
            ]
            (is (nil? set-cookie-value))
            (check-body-error response-body 400)
        )
    )
    (testing "logging in as admin"
        (let [
                response (handler/app (assoc (mock/request :post "/")
                    :params {"payload" (json/write-str {
                        "function" "login",
                        "username" "admin1",
                        "password" "mypass",
                        "token" "12345"})}))
                response-body (json/read-str (:body response) :key-fn keyword)
                set-cookie-value (first ((:headers response) "Set-Cookie"))
            ]
            (is (not (nil? set-cookie-value)))
            (let [cookie (first (process-header set-cookie-value))]
                (is (= (first cookie) "ring-session"))
                (def admin-cookie cookie)
                (check-body-ok response-body)
                (is (contains? (first (vals @notification/tokens)) "12345"))
                (is (contains? @notification/tokens admin-house-id))
            )
        )
    )
    (testing "triggering admin function with proper permissions"
        (let [response-body (json/read-str (:body (handler/app (assoc (mock/request :post "/")
            :params {"payload" (json/write-str {"function" "testAdmin"})}
            :headers {"cookie" (str (first admin-cookie) "=" (second admin-cookie))}
            ))) :key-fn keyword)]
            (check-body-ok response-body)
        )
    )
    (testing "triggering function with improper permissions"
        (let [response-body (json/read-str (:body (handler/app (assoc (mock/request :post "/")
            :params {"payload" (json/write-str {"function" "testController"})}
            :headers {"cookie" (str (first admin-cookie) "=" (second admin-cookie))}
            ))) :key-fn keyword)]
            (check-body-error response-body 403)
        )
    )
    (testing "creating new user"
        (let [
                response-body (json/read-str (:body (handler/app (assoc (mock/request :post "/")
                    :params {"payload" (json/write-str
                        {"function" "newUser", "username" "user1", "password" "userpass"})}
                    :headers {"cookie" (str (first admin-cookie) "=" (second admin-cookie))}
                    ))) :key-fn keyword)
                house-id (:houseId (first (db/select "agent" {"username" "admin1"})))
                inserted-user (first (db/select "agent" {"username" "user1"}))
            ]
            (check-body-ok response-body)
            (is (= house-id (:houseId inserted-user)))
        )
    )
    (testing "associating new controller"
        (let [
                response-body (json/read-str (:body (handler/app (assoc (mock/request :post "/")
                    :params {"payload" (json/write-str
                        {
                            "function" "registerController",
                            "controllerId" controller-id
                        })}
                    :headers {"cookie" (str (first admin-cookie) "=" (second admin-cookie))}
                    ))) :key-fn keyword)
                updated-controller (first (db/select "agent" {:_id (ObjectId. controller-id)}))
                admin (first (db/select "agent" {"username" "admin1"}))
            ]
            (check-body-ok response-body)
            (is (= (:houseId admin) (:houseId updated-controller)))
        )
    )
    (testing "associating inexisting controller"
        (let [
                response-body (json/read-str (:body (handler/app (assoc (mock/request :post "/")
                    :params {"payload" (json/write-str
                        {
                            "function" "registerController",
                            "controllerId" "2"
                        })}
                    :headers {"cookie" (str (first admin-cookie) "=" (second admin-cookie))}
                    ))) :key-fn keyword)
            ]
            (check-body-error response-body 400)
        )
    )
    (testing "logging out admin account"
        (let [response-body (json/read-str (:body (handler/app (assoc (mock/request :post "/")
            :params {"payload" (json/write-str {"function" "logout", "token" "12345"})}
            :headers {"cookie" (str (first admin-cookie) "=" (second admin-cookie))}
            ))) :key-fn keyword)]

            (check-body-ok response-body)
            (is (empty? (first (vals @notification/tokens))))
        )
    )
    (testing "logging out twice in a row"
        (let [response-body (json/read-str (:body (handler/app (assoc (mock/request :post "/")
            :params {"payload" (json/write-str {"function" "logout", "token" "12345"})}
            :headers {"cookie" (str (first admin-cookie) "=" (second admin-cookie))}
            ))) :key-fn keyword)]
            (check-body-error response-body 403)
        )
    )
    (testing "logging in as user"
        (let [
                response (handler/app (assoc (mock/request :post "/")
                    :params {"payload" (json/write-str {
                        "function" "login",
                        "username" "user1",
                        "password" "userpass",
                        "token" "67891"})}))
                response-body (json/read-str (:body response) :key-fn keyword)
                set-cookie-value (first ((:headers response) "Set-Cookie"))
            ]
            (is (not (nil? set-cookie-value)))
            (let [cookie (first (process-header set-cookie-value))]
                (is (= (first cookie) "ring-session"))
                (def user-cookie cookie)
                (check-body-ok response-body)
                (is (some #(contains? % "67891") (vals @notification/tokens)))
            )
        )
    )
    (testing "set token"
        (let [
                response (handler/app (assoc (mock/request :post "/")
                    :params {"payload" (json/write-str {
                        "function" "setToken",
                        "kill-token" "67891"
                        "token" "67890"})}
                    :headers {"cookie" (str (first user-cookie) "=" (second user-cookie))}))
                response-body (json/read-str (:body response) :key-fn keyword)
            ]
            (check-body-ok response-body)
            (is (some #(contains? % "67890") (vals @notification/tokens)))
            (is (every? #(not (contains? % "67891")) (vals @notification/tokens)))
        )
    )
    (testing "triggering user function with proper permissions"
        (let [response-body (json/read-str (:body (handler/app (assoc (mock/request :post "/")
            :params {"payload" (json/write-str {"function" "testUser"})}
            :headers {"cookie" (str (first user-cookie) "=" (second user-cookie))}
            ))) :key-fn keyword)]
            (check-body-ok response-body)
        )
    )
    (testing "logging out user account"
        (let [response-body (json/read-str (:body (handler/app (assoc (mock/request :post "/")
            :params {"payload" (json/write-str {"function" "logout", "token" "67890"})}
            :headers {"cookie" (str (first user-cookie) "=" (second user-cookie))}
            ))) :key-fn keyword)]
            (check-body-ok response-body)
            (is (every? empty? (vals @notification/tokens)))
        )
    )
    (testing "logging in as controller"
        (let [
                response (handler/app (assoc (mock/request :post "/")
                    :params {"payload" (json/write-str {
                        "function" "login",
                        "username" "controller1",
                        "password" "ctrlpass"})}))
                response-body (json/read-str (:body response) :key-fn keyword)
                set-cookie-value (first ((:headers response) "Set-Cookie"))
            ]
            (is (not (nil? set-cookie-value)))
            (let [cookie (first (process-header set-cookie-value))]
                (is (= (first cookie) "ring-session"))
                (def controller-cookie cookie)
                (check-body-ok response-body)
            )
        )
    )
    (testing "triggering controller function with proper permissions"
        (let [response-body (json/read-str (:body (handler/app (assoc (mock/request :post "/")
            :params {"payload" (json/write-str {"function" "testController"})}
            :headers {"cookie" (str (first controller-cookie) "=" (second controller-cookie))}
            ))) :key-fn keyword)]
            (check-body-ok response-body)
        )
    )
    (testing "trying to log in without logging out first"
        (let [
                response (handler/app (assoc (mock/request :post "/")
                    :params {"payload" (json/write-str {
                        "function" "login",
                        "username" "admin1",
                        "password" "mypass"})}
                    :headers {"cookie" (str (first controller-cookie) "=" (second controller-cookie))}))
                response-body (json/read-str (:body response) :key-fn keyword)
                set-cookie-value (first ((:headers response) "Set-Cookie"))
            ]
            (is (nil? set-cookie-value))
            (check-body-error response-body 400)
        )
    )
)

(deftest test-db-functions
    (setup-database-test-db)
    (testing "getting all agents on house-id 1"
        (let [
                result (auth/get-agents "1" :user true :admin true :controller true)
                result-grouped (group-by :type result)
            ]
            (is (= (count result) 4))
            (is (= (count (result-grouped "admin")) 1))
            (is (= (count (result-grouped "user")) 2))
            (is (= (count (result-grouped "controller")) 1))
        )
    )
    (testing "getting all agents on house-id 2"
        (let [
                result (auth/get-agents "2" :user true :admin true :controller true)
                result-grouped (group-by :type result)
            ]
            (is (= (count result) 2))
            (is (= (count (result-grouped "admin")) 1))
            (is (nil? (result-grouped "user")))
            (is (= (count (result-grouped "controller")) 1))
        )
    )
    (testing "getting all agents on inexisting house-id"
        (let [
                result (auth/get-agents "3" :user true :admin true :controller true)
            ]
            (is (= (count result) 0))
        )
    )
    (testing "getting admins on house-id 1"
        (let [
                result (auth/get-agents "1" :admin true)
                result-grouped (group-by :type result)
            ]
            (is (= (count result) 1))
            (is (= (count (result-grouped "admin")) 1))
            (is (nil? (result-grouped "user")))
            (is (nil? (result-grouped "controller")))
        )
    )
)
