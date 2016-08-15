(ns server.handler
	(:require
		[server.utils :as utils]
		[compojure.core :refer :all]
		[compojure.route :as route]
		[ring.middleware.session :refer [wrap-session]]
		[ring.util.response :refer :all]
		[ring.middleware.params :refer [wrap-params]]
		[org.httpkit.server :as kit]
		[ring.middleware.reload :as reload]))

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
	"Params must have the following keys: function, collection, and any key for data."
	(let [function (params "function")]
		(if (nil? function)
			(utils/build-response 400 "No function found!")
			(let [result (utils/call-db-function function params)]
				(if (seq? result)
					(utils/build-response 200 (str (dissoc (first result) :_id)))
					(utils/build-response 200 result))))))

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