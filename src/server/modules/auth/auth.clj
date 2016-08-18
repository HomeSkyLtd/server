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

(defn new-admin [obj _]
    (if (valid-username-password? obj)
        (let [new-house (db/insert "house" {} :return-inserted true)]
            (do
                (db/insert "agent" (assoc obj :type "admin" :houseId (str (:_id new-house))))
                {:status 200}
            )
        )
        {:status 400, :errorMessage "username and password cannot be empty"}
    )
)

(defn login [obj _]
    (if (valid-username-password? obj)
        (let [agent (first (db/select "agent" {"username" (:username obj)}))]
            (if (= (:password obj) (:password agent))
    			{:status 200, :session
                    {
                        :houseId (:houseId agent),
                        :userId (str (:_id agent))
                        :permission (:permission agent)
                    }
                }
                {:status 400, :errorMessage "invalid username/password"}
            )
        )
    )
)
