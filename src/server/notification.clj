(ns server.notification
	(:require [clj-http.client :as client]
			  [org.httpkit.server :as kit]
			  [clojure.data.json :as json]))

;;
;; Keeps track of active websockets channels.
;;
(def agent-channel (atom {}))

;;
;; Pool of threads for non blocking send of notifications to app.
;;
(def thread-pool (agent '()))

;;
;; Keeps token per user's device.
;;
(def tokens (atom {}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 								PRIVATE FUNCTIONS							;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- build-msg
	"Build message to be sent as notification to smartphone."
	[token msg]
	(str "{\"notification\": {\"body\":\"" msg "\"},\"to\":\"" token "\"}"))

(defn- send-on-thread
	"Send a notification in one of the threads of the pool."
	[house-tokens msg]
	(let [auth-key "key=AIzaSyClArUOQgE1rH2ff3DELo6vvmQuWTZ68QA"]
		(map #(client/post "https://fcm.googleapis.com/fcm/send"
			{:body (build-msg % msg)
			 :headers {"Authorization" auth-key "Content-Type" "application/json"}
			}) house-tokens
		)
	)
)

(defn- send-websocket-notification!
	"Sends message to controller-id through a websockets channel. Returns true
	 if successful, and false otherwise."
	[controller-id message]
	;(println @agent-channel)
	(let [channel (@agent-channel controller-id)]
		(if (nil? channel)
			false
			(if (map? message)
				(kit/send! channel (json/write-str message))
				(kit/send! channel message)
			)
		)
	)
)

(defn- send-notification
	"Send notifications in a pool of threads."
	[houseId msg]
	(let [house-tokens (@tokens houseId)]
		(await-for 1000 (send thread-pool concat (send-on-thread house-tokens msg)))))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 								PUBLIC FUNCTIONS							;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;							STATE MODULE FUNCTIONS							;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn notify-new-action
	"Server -> Controller
	Send a notification of new action of a user from server to controller."
	[action]
	(let [controllerId (:controllerId action) 
		  msg {:notification "newAction" :action (dissoc action :controllerId)}]
		(if (send-websocket-notification! controllerId msg)
			{:status 200}
			{:status 410 :errorMessage "WebSocket channel not found."}
		)
	)
)

(defn notify-action-result
	"Server -> App
	Send a notification to user's device with the confirmation of an action done."
	[action-result houseId]
	(if (every? action-result [:result :action])
		(let   [result (if (= (:result action-result) 1) "Success" "Failed")
			  	value  (:value (:action action-result))
			  	nodeId (:nodeId (:action action-result))
			  	msg (str result " to send value " value " to node " nodeId)]
			(if (send-notification houseId msg)
				{:status 200}
				{:status 500 :errorMessage "Thread pool couldn't handle."}
			)
		)
		{:status 400 :errorMessage "Not found the keys result or action"}
	)
)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;							RULE MODULE FUNCTIONS							;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;Auxiliar functions to count the number of new rules per controller

(defn list-controller-ids
	"Return a list of distinct controller ids"
	[rules]
	(distinct (map #(:controllerId %) rules))
)

(defn build-count
	"Count the number of new rules for a given controllerId"
	[rules controllerId]
	{
		:notification "newRules"
	 	:quantity (count (filter #(= controllerId (:controllerId %)) rules))
	}
)

(defn notify-new-rules
	"Server -> Controller
	Notification sent via web socket from server to controller with new rules accepted by the user
	and the rules inserted by the user."
	[rules]
	(let [list-of-ids (list-controller-ids rules)]
		(if (every? true? (map #(send-websocket-notification! % (build-count rules %)) list-of-ids))
			{:status 200}
			{:status 410 :errorMessage "WebSocket channel not found."}
		)
	)
)

(defn notify-learnt-rules
	"Server -> App
	Send a notification to user's device with new learnt rules."
	[houseId rules]
	(if (send-notification houseId (str "New detected rules: " (count rules)))
		{:status 200}
		{:status 500 :errorMessage "Thread pool couldn't handle."}
	)
)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;							NODE MODULE FUNCTIONS							;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn notify-accepted-node
	"Server -> Controller
	Send a notification from server to controller with a new node accepted by the user"
	[obj]
	(let [msg {:notification "acceptedNode" :nodeId (:nodeId obj) :accept (:accept obj)}]
		(if (send-websocket-notification! (:controllerId obj) msg)
			{:status 200 }
			{:status 410 :errorMessage "WebSocket channel not found."}
		)
	)
)

(defn notify-detected-nodes
    "Server -> App
    Send a notification to user's device with how many new detected nodes."
    [houseId nodes]
    (if (send-notification houseId (str "New detected nodes: " (count nodes)))
    	{:status 200}
		{:status 500 :errorMessage "Thread pool couldn't handle."}
	)
)