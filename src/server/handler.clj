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
	(let [count ((request :session {}) :count 0)]
		(assoc 
			(build-response
				200
				(if (zero? count)
						"<h1>Hello, Stranger!</h1>"
						(str "<h1>Hello again (" count ")")))
			:session {:count (inc count)})))

(defn- handler-db [func & params]
	(let [result (apply func params)]
		(if (seq? result)
			(build-response 200 (str (dissoc (first result) :_id)))
			(build-response 200))))

(defn- session? [request]
	"Verify if a request has session number."
	(if (empty? (:session request))
		(build-response 403 "<h1>FORBIDDEN</h1>")
		(build-response 200 "<h3>Session on</h3>")))

(defn- insert-in-db [{params :params}]
	"Insert params from request in the collection specified."
	(db/insert (params "collection") (dissoc params "collection")))

(defn- select-from-db [{params :params}]
	"params must have a collection name, a key and a value, which will match in the database collection."
	(let [collection (params "collection")
			key (params "key")
			value (params "value")]
	(db/select collection key value)))

(defroutes app-routes
  (GET "/" request (handler request))
  (GET "/check" request (session? request))
  (POST "/post" request (handler-db insert-in-db request))
  (POST "/find" request (handler-db select-from-db request))
  (route/not-found "Not Found"))

(def app
	(-> app-routes
		(wrap-session {:cookie-attrs {:max-age 30}})
		(wrap-params)))