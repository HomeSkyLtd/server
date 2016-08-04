(ns server.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))

(defn- str-to [num]
	(apply str (interpose "," (range 1 (inc num)))))

(defn- str-from [num]
	(apply str (interpose "," (reverse (range 1 (inc num))))))

(defroutes app-routes
  (GET "/" [] "Hello World")
  (GET "/count-up/:to" [to] (str-to (Integer. to)))
  (GET "/count-down/:from" [from] (str-from (Integer. from)))
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes site-defaults))
