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

(defn test-handler [_ _ _] {:status 200})

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
	"login" auth/login,
	"logout" auth/logout,
	"newUser" auth/new-user,
	"newAdmin" auth/new-admin,
	"registerController" auth/register-controller,

	"testUser" test-handler
	"testAdmin" test-handler
	"testController" test-handler
	})

; Granting base permission to a function means anyone can access it
(def permissions
	{
		"base" 1,
		"user" 2,
		"controller" 4,
		"admin" 8
	})

;FIXME uncomment when implemented
(def ^:private function-permissions {
	; "newData" (permissions "controller"),
	; "newCommand" (permissions "controller"),
	; "newAction" (permissions "user"),
	; "getHouseState" (permissions "user"),
	;
	"newRules" (bit-or (permissions "admin") (permissions "user")),
	"getRules" (bit-or (permissions "admin") (permissions "user") (permissions "controller")),
	"getLearntRules" (bit-or (permissions "admin") (permissions "user")),
	
	"newDetectedNode" (permissions "controller"),
	"setNodeExtra" (bit-or (permissions "user") (permissions "admin")),
	"getNodes" (bit-or (permissions "user") (permissions "admin")),
	"acceptNode" (bit-or (permissions "user") (permissions "admin")),
	"setNodeState" (permissions "controller"),
	
	"login" (permissions "base"),
	"logout" (bit-or (permissions "admin") (permissions "user") (permissions "controller")),
	"newUser" (permissions "admin"),
	"newAdmin" (permissions "base"),
	"registerController" (permissions "admin"),

	"testUser" (permissions "user")
	"testAdmin" (permissions "admin")
	"testController" (permissions "controller")
	})

(defn- build-response-json
	"Build JSON response. If response-map is not specified, only status and error message
	are included in the response. If status or error message are included in response-map,
	those values will be used when constructing the response."
	([response-map status message]
		(json/write-str (merge {:status status, :errorMessage message} response-map)))
	([status message] (build-response-json {} status message))
	([response-map] (build-response-json response-map 500 ""))
)

(defn- get-session-permission [session]
	(if (nil? (:permission session))
		0
		(permissions (:permission session))
	)
)

(defn- handler [{params :params session :session}]
	(let
		[
			params_map (json/read-str (params "payload") :key-fn keyword)
			function (params_map :function)
			obj (dissoc params_map :function)
			permission (bit-or (permissions "base") (get-session-permission session))
			houseId (:houseId session)
			userId (:userId session)
		]
		; (println session)
		(if (or (nil? function) (nil? (function-handlers function)))
			(build-response-json 400 "No function found!")
			(cond
				;If not authorized due to mismatched authorization
				(zero? (bit-and permission (function-permissions function)))
					(if (= (permissions "base") permission)
						(build-response-json 403 "User not logged in")
						(build-response-json 403 "Unauthorized operation")
					)
				;If everything OK
				:else
					(let [	result ((function-handlers function) obj houseId userId)
							session (:session result)]
						(if (contains? result :session)
							{:status 200 :body (build-response-json (dissoc result :session)) :session session}
							(build-response-json result)
						)
					)
			)
		)
	)
)

(defroutes app-routes
	(GET "/" [] (utils/build-response 200 "Server running"))
	(POST "/" request (handler request))
  	(route/not-found "Not Found"))

(def app
	(-> app-routes
		(reload/wrap-reload)
		(wrap-session {:cookie-attrs {:max-age 30}})
		(wrap-params)))

(defn -main[]
	(println "Running on port 3000")
	(kit/run-server app {:port 3000}))
