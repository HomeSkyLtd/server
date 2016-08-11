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

(defn- insert-in-db [params]
	"Insert params from request in the collection specified."
	(db/insert (params "collection") (dissoc (dissoc params "collection") "query")))

(defn- select-from-db [params]
	"params must have a collection name, a key and a value, which will match in the database collection."
	(let [collection (params "collection")
		key (params "key")
		value (params "value")]
		(db/select collection key value)))

(def db-functions {"insert" insert-in-db, "select" select-from-db})

(defn- session? [request]
	"Verify if a request has session number."
	(if (empty? (:session request))
		(build-response 403 "<h1>FORBIDDEN</h1>")
		(build-response 200 "<h3>Session on</h3>")))

(defn- handler [request]
	(let [count ((request :session {}) :count 0)]
		(assoc 
			(build-response
				200
				(if (zero? count)
					"<h1>Hello, Stranger!</h1>"
					(str "<h1>Hello again (" count ")")))
			:session {:count (inc count)})))

(defn- handler-db [{params :params}]
	(let [func (db-functions (params "query")) result (func params)]
		(if (seq? result)
			(build-response 200 (str (dissoc (first result) :_id)))
			(build-response 200))))

(defroutes app-routes
	(GET "/" request (handler request))
	(GET "/check" request (session? request))
	(POST "/db" request (handler-db request))
  (route/not-found "Not Found"))

(def app
	(-> app-routes
		(wrap-session {:cookie-attrs {:max-age 120}})
		(wrap-params)))