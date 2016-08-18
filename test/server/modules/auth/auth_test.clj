(ns server.modules.auth.auth-test
  (:use [clojure.test])
  (:use [server.modules.node.node])
  (:require
      [server.db :as db]
      [server.handler :as handler :only [app]]
      [ring.mock.request :as mock]
      [clojure.data.json :as json]
      [clojure.string :only [split join] :as str]
      (monger [core :as mg] [collection :as mc] [result :as res] [db :as md]))
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

(deftest test-app
    (md/drop-db db/db)
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
            (is (not (nil? inserted-admin)))
            (is (not (nil? inserted-house)))
            (is (= (:password inserted-admin) "mypass"))
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
    (testing "logging in as admin"
        (let [
                response (handler/app (assoc (mock/request :post "/")
                    :params {"payload" (json/write-str {
                        "function" "login",
                        "username" "admin1",
                        "password" "mypass"})}))
                response-body (json/read-str (:body response) :key-fn keyword)
                set-cookie-value (first ((:headers response) "Set-Cookie"))
            ]
            (is (not (nil? set-cookie-value)))
            (let [cookie (first (process-header set-cookie-value))]
                (is (= (first cookie) "ring-session"))
                (def admin-cookie cookie)
                (check-body-ok response-body)
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
        (db/insert "agent" {:username "controller1", :password "ctrlpass",
            :type "controller", :controllerId 1, :houseId ""})
        (let [
                response-body (json/read-str (:body (handler/app (assoc (mock/request :post "/")
                    :params {"payload" (json/write-str
                        {
                            "function" "registerController",
                            "controllerId" 1
                        })}
                    :headers {"cookie" (str (first admin-cookie) "=" (second admin-cookie))}
                    ))) :key-fn keyword)
                updated-controller (db/select "agent" {"controllerId" 1})
                admin (db/select "agent" {"username" "admin1"})
            ]
            (check-body-ok response-body)
            (is (= (:house-id admin) (:house-id updated-controller)))
        )
    )
    (testing "logging out"
        (let [response-body (json/read-str (:body (handler/app (assoc (mock/request :post "/")
            :params {"payload" (json/write-str {"function" "logout"})}
            :headers {"cookie" (str (first admin-cookie) "=" (second admin-cookie))}
            ))) :key-fn keyword)]
            (check-body-ok response-body)
        )
    )
    (testing "logging out twice in a row"
        (let [response-body (json/read-str (:body (handler/app (assoc (mock/request :post "/")
            :params {"payload" (json/write-str {"function" "logout"})}
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
                        "password" "userpass"})}))
                response-body (json/read-str (:body response) :key-fn keyword)
                set-cookie-value (first ((:headers response) "Set-Cookie"))
            ]
            (is (not (nil? set-cookie-value)))
            (let [cookie (first (process-header set-cookie-value))]
                (is (= (first cookie) "ring-session"))
                (def user-cookie cookie)
                (check-body-ok response-body)
            )
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
)
