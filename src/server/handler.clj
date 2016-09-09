(ns ^{:doc "Receive requests and call all modules' functions"}
	server.handler
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
		[try-let :refer [try-let]]
		[server.modules.auth.auth :as auth]
		[server.modules.node.node :as node]
		[server.modules.state.state :as state]
		[server.modules.rule.rule :as rule]
		[server.notification :as notification]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; STATE REFERENCES
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^{:doc "Keeps session data"} session-storage (atom {}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; HANDLER CALLBACK FUNCTIONS
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn test-handler 
	"Test handler - returns status 200"
	[_ _ _] {:status 200})


(def ^:private 
	 ^{:doc "Map of functions to their names."}
	function-handlers {
		"newData" state/new-data,
		"newCommand" state/new-command,
		"newAction" state/new-action,
		"getHouseState" state/get-house-state,

		"newRules" rule/new-rules,
		"getRules" rule/get-rules,
		"getLearntRules" rule/get-learnt-rules,

		"newDetectedNodes" node/new-detected-nodes,
		"setNodeExtra" node/set-node-extra,
		"getNodesInfo" node/get-nodes-info,
		"acceptNode" node/accept-node,
	    "removeNode" node/remove-node,
		"setNodeState" node/set-node-state,

		"login" auth/login,
		"logout" auth/logout,
		"newUser" auth/new-user,
		"newAdmin" auth/new-admin,
		"registerController" auth/register-controller,
		"setToken" auth/set-token,

		"testBase" test-handler,
		"testUser" test-handler,
		"testAdmin" test-handler,
		"testController" test-handler
	})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; HANDLER CALLBACK FUNCTIONS - PERMISSIONS
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(def ^{:doc "Granting base permission to a function means anyone can access it"}
	permissions
	{
		"base" 1,
		"user" 2,
		"controller" 4,
		"admin" 8
	})

(def ^:private 
	 ^{:doc "List of permissions to access each function."}
	function-permissions {
		"newData" (permissions "controller"),
		"newCommand" (permissions "controller"),
		"newAction" (bit-or (permissions "user") (permissions "admin")),
		"getHouseState" (bit-or (permissions "user") (permissions "admin")),

		"newRules" (bit-or (permissions "admin") (permissions "user")),
		"getRules" (bit-or (permissions "admin") (permissions "user") (permissions "controller")),
		"getLearntRules" (bit-or (permissions "admin") (permissions "user")),

		"newDetectedNodes" (permissions "controller"),
		"setNodeExtra" (bit-or (permissions "user") (permissions "admin")),
		"getNodesInfo" (bit-or (permissions "user") (permissions "admin")),
		"acceptNode" (bit-or (permissions "user") (permissions "admin")),
		"removeNode" (bit-or (permissions "user") (permissions "admin")),
	    "setNodeState" (permissions "controller"),

		"login" (permissions "base"),
		"logout" (bit-or (permissions "admin") (permissions "user") (permissions "controller")),
		"newUser" (permissions "admin"),
		"newAdmin" (permissions "base"),
		"registerController" (permissions "admin"),
		"setToken" (bit-or (permissions "admin") (permissions "user")),

		"testBase" (permissions "base"),
		"testUser" (permissions "user"),
		"testAdmin" (permissions "admin"),
		"testController" (permissions "controller")
	})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; HELPER FUNCTIONS
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- build-response-json
	"Build JSON response. If response-map is not specified, only status and error message
	are included in the response. If status or error message are included in response-map,
	those values will be used when constructing the response."
	([response-map status message]
		(json/write-str (merge {:status status, :errorMessage message} response-map)))
	([status message] 
		(build-response-json {} status message))
	([response-map] 
		(build-response-json response-map 500 ""))
)

(defn- get-session-permission 
	"Check if there is a permission in current session."
	[session]
	(if (nil? (:permission session))
		0
		(permissions (:permission session))
	)
)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; HANDLERS
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn- handler 
	"POST requests handler"
	[{params :params session :session}]
	(try-let
		[
			params_map (json/read-str (params "payload") :key-fn keyword)
			function (params_map :function)
			obj (dissoc params_map :function)
			permission (bit-or (permissions "base") (get-session-permission session))
			houseId (:houseId session)
			agentId (:agentId session)
		]
		(if (or (nil? function) (nil? (function-handlers function)))
			(build-response-json 400 "No function found!")
			;If not authorized due to mismatched authorization
			(if (zero? (bit-and permission (function-permissions function)))
				(if (= (permissions "base") permission)
					(build-response-json 403 "User not logged in")
					(build-response-json 403 "Unauthorized operation")
				)
				(let [result ((function-handlers function) obj houseId agentId)
					  session (:session result)
					  token (:token result)
					  kill-token (:kill-token result)]
					
					; Handle token creation / destruction
					(if (not (nil? (first (vals token))))
						(if (contains? @notification/tokens (first (keys token)))
							(swap! notification/tokens assoc 
								(first (keys token)) 
								(conj (@notification/tokens (first (keys token))) (first (vals token)))
							)
							(swap! notification/tokens assoc
								(first (keys token))
								#{(first (vals token))}
							)
						)
					)
					(if (not (nil? kill-token))
						(if (contains? @notification/tokens (first (keys kill-token)))
							(swap! notification/tokens assoc 
								(first (keys kill-token)) 
								(disj (@notification/tokens (first (keys kill-token))) (first (vals kill-token)))
							)
							(println "Warning: trying to delete inexisting token")
						)
					)
					(if (contains? result :session)
						{:status 200 :body (build-response-json (dissoc result :session :token :kill-token)) :session session}
						(build-response-json (dissoc result :token :kill-token))
					)
				)
			)
		)
		(catch Exception e (build-response-json 400 (str "Invalid POST data caused " e)))
	)
)

(defn ws-handler 
	"Web Sockets handler"
	[request]
	(let
		[
			session (:session request)
			houseId (:houseId session)
			agentId (:agentId session)
			permission (bit-or (permissions "base") (get-session-permission session))
		]
		;If not authorized due to mismatched authorization
		(if (zero? (bit-and permission (permissions "controller")))
			(if (= (permissions "base") permission)
				{:status 403 :headers {"X-WebSocket-Reject-Reason" "Not logged in"}}
				{:status 403 :headers {"X-WebSocket-Reject-Reason" "Unauthorized operation"}}
			)
			(kit/with-channel request channel
				(swap! notification/agent-channel assoc agentId channel)
				(println "Received websockets call")
				(println (str "active channels: " (count @notification/agent-channel)))
				(println @notification/agent-channel)
				(kit/on-close channel (fn [status]
					(swap! notification/agent-channel dissoc (first (utils/find-keys @notification/agent-channel channel)))
					(println "channel closed: " status)
					(println (str "active channels: " (count @notification/agent-channel)))
				))
				; (on-receive channel (fn [data] ;; echo it back
				;                   (println (str "Received data: " data))
				;                   (send! channel data)))
			)
		)
	)
)

























;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; SERVER CONFIGURATION
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defroutes app-routes
	(GET "/" [] (utils/build-response 200 "Server running"))
	(POST "/" request (handler request))
	(GET "/ws" request (ws-handler request))
  	(route/not-found "Not Found"))

(def app
	(-> app-routes
		(reload/wrap-reload)
		(wrap-session {:cookie-attrs {:max-age 20} :store (ring.middleware.session.memory/memory-store session-storage)})
		(wrap-params)))

(defn -main[]
	(println "Running on port 3000")
	(kit/run-server app {:port 3000}))
