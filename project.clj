(defproject server "1.0.0"
  :description "Server running machine learning algorithms and communication 
                protocols between itself, controllers and mobile devices inside 
                the HomeSky system."
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
                 [org.rosuda.REngine/REngine "2.1.0"]
                 [org.rosuda.REngine/Rserve "1.8.1"]
                 [clj-http "3.2.0"]]
  :main server.handler
  :plugins [[lein-ring "0.9.7"]
            [lein-codox "0.9.7"]]
  :ring {:handler server.handler/app}
  :codox {
    :metadata {:doc "I am probably a variable (but also a function)"}
    :namespaces [#"^server.modules\."]
  }
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.0"]
                        [ring/ring-devel "1.1.8"]]}})
