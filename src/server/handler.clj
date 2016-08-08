(ns server.handler
  (:require
  	[server.db :as db]
  	[compojure.core :refer :all]
    [compojure.route :as route]
    [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
    [ring.middleware.session :refer [wrap-session]]
	[ring.util.response :refer :all]
	[ring.middleware.params :refer [wrap-params]]))

(defn- build-response [status body]
	"Build response with status and body"
	{:status status
	 :headers {"Content-Type" "text/html"}
	 :body body})

(defn- handler [request]
	(let [count ((request :session {}) :count 0)]
		(assoc 
			(build-response
				200
				(if (zero? count)
						(str request "</br><h1>Hello, Stranger!</h1>")
						(str request "</br><h1>Hello again (" count ")")))
			:session {:count (inc count)})))

(defn- session? [request]
	"Verify if a request has session number."
	(if (empty? (:session request))
		(build-response 403 (str "<h1>FORBIDDEN</h1>" request))
		(build-response 200 (str "<h3>Session on</h3>" request))))

(defn- post-params [request]
	"Return a map with parameters from POST request"
	(let [name ((:params request) "name") id ((:params request) "id")]
		{:name name :id id}))

(defn- insert-in-db [{params :params} coll-name]
	"Insert params from request in the collection specified."
	(db/insert params coll-name))

(defroutes app-routes
  (GET "/" request (handler request))
  (GET "/check" request (session? request))
  (POST "/post" request (insert-in-db request "test-table"))
  (route/not-found "Not Found"))

(def app
	(-> app-routes
		(wrap-session {:cookie-attrs {:max-age 30}})
		(wrap-params)))