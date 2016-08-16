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
		[clojure.walk :as walk]
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

(def permissions
	{
		"user" 1,
		"controller" 2,
		"admin" 4
	})

(def ^:private function-permissions {
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

	"test" (bit-or (permissions "user") (permissions "admin"))
	})

(defn- build-response-json
	([response-map status message]
		(json/write-str (merge response-map {:status status, :errorMessage message})))
	([status message] (build-response-json {} status message))
)

(defn- handler [{params :params}]
	(let
		[
			params_map (json/read-str (params "payload") :key-fn keyword)
			function (params_map :function)
			obj (dissoc params_map :function)
			permission (permissions "controller") ;FIXME get this from session
		]

		(if (or (nil? function) (nil? (function-handlers function)))
			(build-response-json 400 "No function found!")
			(cond
				;If user not logged
				(nil? permission)
					(build-response-json 403 "User not logged in")
				;If user logged, but triggering an unauthorized operation
				(zero? (bit-and permission (function-permissions function)))
					(build-response-json 403 "Unauthorized operation")
				;If everything OK
				:else
					(let [result ((function-handlers function) obj)]
						(build-response-json result 200 "")
					)
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
