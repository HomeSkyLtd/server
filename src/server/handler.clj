(ns server.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.session :refer :all]
			[ring.util.response :refer :all]))

(defn handler [{session :session}]
	(response (str "Hello" (:username session))))

(defroutes app-routes
  (GET "/" [] "Hello World")
  (route/not-found "Not Found"))

;(def app
;  (wrap-defaults app-routes site-defaults))
(def app
	(wrap-session handler))
