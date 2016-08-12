(ns server.handler
	(:require
		[server.utils :as utils]
		[compojure.core :refer :all]
		[compojure.route :as route]
		[ring.middleware.session :refer [wrap-session]]
		[ring.util.response :refer :all]
		[ring.middleware.params :refer [wrap-params]]
		[org.httpkit.server :as kit]
		[ring.middleware.reload :as reload]
		[clojure.data.json :as json]))

(defn- handler-login [request]
	(let [count ((request :session {}) :count 0)]
		(assoc 
			(utils/build-response
				200
				(if (zero? count)
					"<h1>Hello, Stranger!</h1>"
					(str "<h1>Hello again (" count ")")))
			:session {:count (inc count)})))

(defn- handler [{params :params}]
	"Params must have the following keys: method, collection, and any key for data."
	(let [data (json/read-str (params "data"))
			method (data "method")
			values (dissoc data "method")]
		(if (or (nil? method) (nil? (values "collection")))
			(utils/build-response 400 "No method found!")
			(let [func (utils/db-functions method) result (func values)]
				(if (seq? result)
					(utils/build-response 200 (str (dissoc (first result) :_id)))
					(utils/build-response 200 "Ok"))))))

(defroutes app-routes
	(GET "/" [] (utils/build-response 200 "Server running"))
	(POST "/" request (handler request))
	(GET "/login" request (handler-login request))
	(GET "/check" request (utils/session? request))
  	(route/not-found "Not Found"))

(def app
	(-> app-routes
		(reload/wrap-reload)
		(wrap-session {:cookie-attrs {:max-age 30}})
		(wrap-params)))

(defn -main[] 
	(println "Running on port 3000")
	(kit/run-server app {:port 3000}))