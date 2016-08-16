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
		[clojure.data.json :as json]
		[server.modules.auth.auth :as auth]
		[server.modules.node.node :as node]
		[server.modules.state.state :as state]
		[server.modules.rule.rule :as rule]))

(defn- handler-login [request]
	(let [count ((request :session {}) :count 0)]
		(assoc
			(utils/build-response
				200
				(if (zero? count)
					"<h1>Hello, Stranger!</h1>"
					(str "<h1>Hello again (" count ")")))
			:session {:count (inc count)})))

(defn test-handler [obj] obj)

;FIXME uncomment when implemented
(def ^:private function-handlers {
	; "newData" state/new-data,
	; "newCommand" state/new-command,
	; "newAction" state/new-action,
	; "getHouseState" state/get-house-state,
	;
	; "newRules" rule/new-rules,
	; "getRules" rule/get-rules,
	; "getLearntRules" rule/get-learnt-rules,
	;
	; "newDetectedNode" node/new-detected-node,
	; "setNodeExtra" node/set-node-extra,
	; "getNodes" node/get-nodes,
	; "acceptNode" node/accept-node,
	; "setNodeState" node/set-node-state,
	;
	; "login" auth/login,
	; "logout" auth/logout,
	; "newUser" auth/new-user,

	"test" test-handler
	})

(defn- handler [{params :params}]
	(let
		[
			function (params "function")
			obj (dissoc params "function")
		]
		(if (or (nil? function) (nil? (function-handlers function)))
			(utils/build-response 400 "No function found!")
			(let [result ((function-handlers function) obj)]
				(json/write-str result)
			)
		)
	)
)

(defroutes app-routes
	(GET "/" [] (utils/build-response 200 "Server running"))
	(POST "/" request (handler request))
	;(POST "/" request (fn [request] (println request) (handler request)))
  	(route/not-found "Not Found"))

(def app
	(-> app-routes
		(reload/wrap-reload)
		(wrap-session {:cookie-attrs {:max-age 30}})
		(wrap-params)))

(defn -main[]
	(println "Running on port 3000")
	(kit/run-server app {:port 3000}))
