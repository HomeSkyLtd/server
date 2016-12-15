(ns ^{:doc "Functions to send notifications to controllers and mobile devices."}
	server.notification
	(:require [clj-http.client :as client]
			  [org.httpkit.server :as kit]
			  [clojure.data.json :as json]))

(def ^{:doc "Atom that keeps track of active websockets channels."} 
	agent-channel (atom {}))

(def ^{:doc "Keeps track of pending notifications to controllers."}
	pending-ws-notifications (atom {}))

(def ^{:doc "Agent that is a pool of threads for non blocking send of notifications to app."} 
	thread-pool (agent true))

(def ^{:doc "Keeps token per user's device."} 
	tokens (atom {}))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;															;;
;;				 DEVELOPMENT TEST FUNCTIONS                 ;;
;;															;;
;;;;;;;;;These fuctions will be removed in production;;;;;;;;;
;;															;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn init-tokens
	"Test function to initialize tokens for the developers' devices' tokens"
	[]
	(let [token1 "c5l32T5w01o:APA91bHs4fI8QJTkcimkU4HVrMscJ1nNsaszuEYmerp0AZIIXIQStbTp1uQOg5J6JdATkGfNvg3lWBevyd6ezjE_TPb4-1vMhYDtemSu8ttH36DwxyB0W7JajdmlX6qyCjoJ5zYnWOkN"
	          house-tokens {1 #{token1}}]
		(reset! tokens house-tokens)
	)
)




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 								PRIVATE FUNCTIONS							;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- build-msg
	"Build message to be sent as notification to smartphone."
	[token msg]
	(json/write-str {:data {:data msg} :to token})
)

; Former implementation that runs requests on a thread. This is not needed, since calls to 
; send-on-thread are already made within a dedicated thread or from an agent

; (defn- send-on-thread
; 	"Send a notification in one of the threads of the pool."
; 	[houseId msg]
; 	(let [house-tokens (@tokens houseId)
; 		  auth-key "key=AIzaSyCx7vgnhSyCAwqLaFC59w6Axsmqq5Yrz1M"
; 		  url "https://fcm.googleapis.com/fcm/send"
; 		  headers {"Authorization" auth-key "Content-Type" "application/json"}
; 		  result (promise)]
; 		(doseq [house-token house-tokens]
; 			(future (deliver result (client/post url {:body (build-msg house-token msg) :headers headers})))
; 			(if (= 0 ((json/read-str (:body @result)) "success"))
; 				(swap! tokens #(update % houseId disj house-token))
; 			)
; 		)
; 	)
; )

(defn- send-on-thread
	"Send a notification (no theads involved)."
	[houseId msg]
	(let
		[
			house-tokens (@tokens houseId)
			auth-key "key=AIzaSyCx7vgnhSyCAwqLaFC59w6Axsmqq5Yrz1M"
			url "https://fcm.googleapis.com/fcm/send"
			headers {"Authorization" auth-key "Content-Type" "application/json"}
		]
		(doseq [house-token house-tokens]
			(let [result
						(try
							(client/post url {:body (build-msg house-token msg) :headers headers})
							(catch Exception e (println "Fail to connect to Firebase")))]
				(if result
					(if (= 0 ((json/read-str (:body result)) "success"))
						(swap! tokens #(update % houseId disj house-token))))
			)
		)
	)
)


(defn- send-notification
	"Send notifications in a pool of threads."
	[houseId msg]
	(let [timeout (* 10 1000)]
		(send thread-pool (fn [_] (send-on-thread houseId msg)))
	)
)


(defn- send-websocket-notification!
	"Sends message to controller-id through a websockets channel. Returns true
	 if successful, and false otherwise."
	[controller-id message]
	(let [channel (@agent-channel controller-id)]
		(if (nil? channel)
			(if (contains? @pending-ws-notifications controller-id)
				(swap! pending-ws-notifications assoc controller-id (conj (@pending-ws-notifications controller-id) message))
				(swap! pending-ws-notifications assoc controller-id (list message))
			)
			(if (map? message)
				(kit/send! channel (json/write-str message))
				(kit/send! channel message)
			)
		)
	)
)


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
	(if (and
			(every? action-result [:result :action])
			(every? (:action action-result) [:value :nodeId :controllerId :commandId]))
		(let [msg (json/write-str (assoc action-result :notification "actionResult"))]
			(send-notification houseId msg)
			{:status 200}
		)
		{:status 400 :errorMessage "Not found the keys result or action"}
	)
)

(defn notify-new-data
  "Server -> App
  Send a push to user's device to autoatically update the data value on screen"
  [obj houseId]
  (if (every? identity (map #(every? % [:nodeId :dataId :value]) obj))
    (let [msg (json/write-str {:notification "newData" :data obj})]
      (send-notification houseId msg)
      {:status 200})
    {:status 400 :errorMessage "Not found nodeId or dataId or value"}))

(defn notify-new-command
  "Server -> App
  Send a push to user's device to autoatically update the command value on screen"
  [obj houseId]
  (if (every? identity (map #(every? % [:nodeId :commandId :value]) obj))
    (let [msg (json/write-str {:notification "newCommand" :command obj})]
      (send-notification houseId msg)
      {:status 200})
    {:status 400 :errorMessage "Not found nodeId or commandId or value"}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;							RULE MODULE FUNCTIONS							;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;Auxiliar functions to count the number of new rules per controller

(defn list-controller-ids
	"Return a list of distinct controller ids"
	[rules]
	(distinct (map :controllerId rules))
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
		(doseq [sending-id list-of-ids]
			(send-websocket-notification! sending-id (build-count rules sending-id))
		)
		{:status 200}
	)
)

(defn notify-learnt-rules
	"Server -> App
	Send a notification to user's device with new learnt rules."
	[houseId rule-count]
	(send-notification houseId (json/write-str {:notification "newRules" :quantity rule-count}))
	{:status 200}
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

(defn notify-removed-node
	"Server -> Controller
	Notify the controller that a node was removed by the user throgh the app."
	[obj]
	(let [msg {:notification "removedNode" :nodeId (:nodeId obj)}]
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
    (send-notification houseId (json/write-str {:notification "detectedNode" :quantity (count nodes)}))
    {:status 200}
)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;							   HELPER  FUNCTIONS							;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn send-pending-notifications!
	"
		Sends pending notifications, if any, to the specified controller. Clears the
		queue of pending notifications if successful.
	"
	[controller-id]
	(if (not (empty? (@pending-ws-notifications controller-id)))
		(let [messages (@pending-ws-notifications controller-id)]
			(swap! pending-ws-notifications dissoc controller-id)
			(doseq [message messages]
				(send-websocket-notification! controller-id message)
			)
		)
	)
)
