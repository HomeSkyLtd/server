(defproject server "1.0.0"
  :description "Server running machine learning algorithms."
  :url "https://github.com/HomeSkyLtd/server"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [compojure "1.4.0"]
                 [ring/ring-defaults "0.1.5"]
                 [com.novemberain/monger "3.0.2"]
                 [http-kit "2.2.0"]
                 [org.clojure/data.json "0.2.6"]
                 [com.novemberain/validateur "2.5.0"]
                 [crypto-password "0.2.0"]
                 [try-let "1.1.0"]
                 [org.slf4j/slf4j-nop "1.7.12"]
                 [clj-http "3.2.0"]
                 ]
  :main server.handler
  :plugins [[lein-ring "0.9.7"]
            [lein-codox "0.9.7"]]
  :ring {:handler server.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.0"]
                        [ring/ring-devel "1.1.8"]]}})
