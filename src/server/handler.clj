(ns server.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.session :refer [wrap-session]]
			[ring.util.response :refer :all]))

(defn handler [request]
	(let [count ((request :session {}) :count 0)]
		{
			:status 200
			:headers {"Content-Type" "text/html"}
			:body (if (zero? count)
						(str request "</br><h1>Hello, Stranger!</h1>")
						(str request "</br><h1>Hello again (" count ")"))
			:session {:count (inc count)}
		}))

(defn- session? [request]
	(if (empty? (:session request))
		{:status 403
		 :headers {"Content-Type" "text/html"}
		 :body (str "<h1>FORBIDDEN</h1>" request)}
		{:status 200
		 :headers {"Content-Type" "text/html"}
		 :body (str "<h3>Session on</h3>" request)}))

(defroutes app-routes
  (GET "/" request (handler request))
  (GET "/check" request (session? request))
  (route/not-found "Not Found"))

(def app 
	(wrap-session app-routes {:cookie-attrs {:max-age 30}}))