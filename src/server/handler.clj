(ns server.handler
  (:require
  	[server.db :as db]
  	[compojure.core :refer :all]
    [compojure.route :as route]
    [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
    [ring.middleware.session :refer [wrap-session]]
	[ring.util.response :refer :all]
	[ring.middleware.params :refer [wrap-params]]))

(defn- build-response
	"Build response with status and body"
	([status body] 
		{:status status
		 :headers {"Content-Type" "text/html"}
		 :body body})
	([status] 
		{:status status
		 :headers {"Content-Type" "text/html"}}))

(defn- handler [request]
	(if (db/no-db?) (db/init-db))
	(let [count ((request :session {}) :count 0)]
		(assoc 
			(build-response
				200
				(if (zero? count)
						(str request "</br><h1>Hello, Stranger!</h1>")
						(str request "</br><h1>Hello again (" count ")")))
			:session {:count (inc count)})))

(defn- handler-db [func & params]
	(apply func params)
	(build-response 200))

(defn- session? [request]
	"Verify if a request has session number."
	(if (empty? (:session request))
		(build-response 403 (str "<h1>FORBIDDEN</h1>" request))
		(build-response 200 (str "<h3>Session on</h3>" request))))

(defn- insert-in-db [{params :params} coll-name]
	"Insert params from request in the collection specified."
	(if (db/no-db?) (db/init-db))
	(db/insert params coll-name))

(defroutes app-routes
  (GET "/" request (handler request))
  (GET "/check" request (session? request))
  (POST "/post" request (handler-db insert-in-db request "testTable"))
  (route/not-found "Not Found"))

(def app
	(-> app-routes
		(wrap-session {:cookie-attrs {:max-age 30}})
		(wrap-params)))