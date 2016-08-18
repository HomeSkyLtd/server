(ns server.modules.auth.auth
    (:require
        [server.db :as db]))

(defn- valid-username-password? [obj]
    (if (or
            (nil? (:username obj))
            (nil? (:password obj))
            (empty? (:username obj))
            (empty? (:password obj))
        )
        false
        true
    )
)

(defn new-admin [obj _ _]
    (if (valid-username-password? obj)
        (let [new-house (db/insert "house" {} :return-inserted true)]
            (if (nil? new-house)
                {:status 500, :errorMessage "Failed to insert house in db"}
                (let [inserted (db/insert? "agent" (assoc obj :type "admin" :houseId (str (:_id new-house))))]
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

(defn login [obj house-id _]
    (if (nil? house-id)
        (if (valid-username-password? obj)
            (let [agent (db/select "agent" {"username" (:username obj)})]
                (cond
                    (nil? agent)
                        {:status 500 :errorMessage "could not retrieve agent data"}
                    (empty? agent)
                        {:status 400, :errorMessage "invalid username/password"}
                    :else
                        (if (= (:password obj) (:password (first agent)))
                            {:status 200, :session
                                {
                                    :houseId (:houseId (first agent)),
                                    :userId (str (:_id (first agent)))
                                    :permission (:type (first agent))
                                }
                            }
                        )
                )
            )
        )
        {:status 400 :errorMessage "already logged in, log out first"}
    )

        {:status 400 :errorMessage "username/password cannot be blank"}
    )
)

(defn new-user [obj house-id _]
    (if (valid-username-password? obj)
            (let [inserted (db/insert? "agent" (assoc obj :type "user" :houseId house-id))]
                (if inserted
                    {:status 200}
                    {:status 500 :errorMessage "could not insert new user"}
                )
            )
        {:status 400, :errorMessage "username and password cannot be empty"}
    )
)


(defn logout [obj _ _]
    {:status 200, :session nil}
)

(defn register-controller [obj house-id _]
    (let [controller (db/select "agent" {"controllerId" (:controllerId obj)})]
        (if (empty? controller)
            {:status 400, :errorMessage "invalid controller specified"}
            (do
                (db/update "agent" {:controllerId (:controllerId controller)}
                    :set {"houseId" house-id})
                {:status 200}
            )
        )
    )
)
