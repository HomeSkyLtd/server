(ns ^{:doc "Ensures users' authentication"}
    server.modules.auth.auth
    (:require
        [monger.operators :refer [$in $and]]
        [server.db :as db]
        [crypto.password.pbkdf2 :as passhash]
        [try-let :refer [try-let]])
    (:import org.bson.types.ObjectId)
)

; ------------------------------------------------------------------------------
; HELPER FUNCTIONS FOR HANDLERS
(defn- valid-username-password? 
    [obj]
    (not (or
            (nil? (:username obj))
            (nil? (:password obj))
            (empty? (:username obj))
            (empty? (:password obj))
        )
    )
)

; ------------------------------------------------------------------------------
; FUNCTION HANDLERS
; todo: check if specified username already exists
(defn new-admin
    "Create new admin associated with a new house-id"
    [obj _ _]
    (if (valid-username-password? obj)
        (let [new-house (db/insert "house" {} :return-inserted true)]
            (if (nil? new-house)
                {:status 500, :errorMessage "Failed to insert house in db"}
                (let [inserted (db/insert? "agent"
                    (assoc obj
                        :type "admin"
                        :houseId (str (:_id new-house))
                        :password (passhash/encrypt (:password obj))
                    ))]
                    (if inserted
                        {:status 200}
                        {:status 500, :errorMessage "failed to insert admin in db"}
                    )
                )
            )
        )
        {:status 400, :errorMessage "username and password cannot be empty"}
    )
)

(defn login
    "
        Log in agent and sets a cookie in the response. If some agent is already
        logged in, this handler returns 400 and does nothing with the session.
    "
    [obj house-id _]
    (if (nil? house-id)
        (if (valid-username-password? obj)
            (let 
                [
                    agent-obj (db/select "agent" {"username" (:username obj)})
                    token (obj :token)
                ]
                (cond
                    (nil? agent-obj)
                        {:status 500 :errorMessage "could not retrieve agent data"}
                    (empty? agent-obj)
                        {:status 400, :errorMessage "invalid username/password"}
                    :else
                        (if (passhash/check (:password obj) (:password (first agent-obj)))
                            {
                                :status 200, 
                                :session
                                    {
                                        :houseId (:houseId (first agent-obj)),
                                        :agentId (str (:_id (first agent-obj)))
                                        :permission (:type (first agent-obj))
                                    }
                                :token {(keyword (str (:_id (first agent-obj)))) token}
                            }
                            {:status 400, :errorMessage "invalid username/password"}
                        )
                )
            )
            {:status 400 :errorMessage "username/password cannot be blank"}
        )
        {:status 400 :errorMessage "already logged in, log out first"}
    )

)

(defn new-user
    "Create new user agent, associated with the agent's house-id"
    [obj house-id _]
    (if (valid-username-password? obj)
            (let [inserted (db/insert? "agent"
                (assoc obj
                    :type "user"
                    :houseId house-id
                    :password (passhash/encrypt (:password obj))
                ))]
                (if inserted
                    {:status 200}
                    {:status 500 :errorMessage "could not insert new user"}
                )
            )
        {:status 400, :errorMessage "username and password cannot be empty"}
    )
)


(defn logout
    "Logs user out, sets session to nil"
    [obj _ agent-id]
    {:status 200, :session nil, :kill-token {(keyword agent-id) (obj :token)}}
)

(defn register-controller
    "Associate a controller with the admin's house-id"
    [obj house-id _]
    (try-let [controller (db/select "agent" {:_id (ObjectId. (obj :controllerId))})]
        (if (empty? controller)
            {:status 400, :errorMessage "invalid controller specified"}
            (if (db/update? "agent" {:_id (ObjectId. (obj :controllerId))} :set {"houseId" house-id})
                {:status 200}
                {:status 500 :errorMessage "DB did not update value."}
            )
        )
        (catch IllegalArgumentException e {:status 400, :errorMessage "invalid controller specified"})
    )
)

(defn set-token
    "Change a token to a new value"
    [obj _ agent-id]
    (let [parse (fn [key] {key {(keyword agent-id) (key obj)}})]
            (merge {:status 200} (parse :kill-token) (parse :token))))

; ------------------------------------------------------------------------------
; PRIVATE HELPERS FOR DB FUNCTIONS
(defn- get-condition 
    "Private helpers for DB functions."
    [flags]
    (let [agent-types ["admin" "user" "controller"]]
        (map second (filter #(true? (first %)) (map vector flags agent-types)))
    )
)

; ------------------------------------------------------------------------------
; PUBLIC AGENT DB FUNCTIONS
(defn get-agents 
    "Public agent DB functions"
    [house-id &{:keys [admin user controller]
    :or {admin false, user false, controller false}}]
    (let [condition (get-condition [admin user controller])]
        (db/select "agent" {$and [{"type" {$in condition}} {"houseId" house-id}]})
    )
)
