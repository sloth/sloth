(ns openslack.web
  (:require [com.stuartsierra.component :as component]
            [qbits.jet.server :refer [run-jetty]]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [compojure.response :refer [render]]
            [openslack.web.home :as home]))

(defn- make-routes-handler
  [config]
  (routes
   (GET "/" [] home/home-ctrl)
   (route/resources "/static")
   (route/not-found "<h1>Page not found</h1>")))

(defrecord Web [config]
  component/Lifecycle
  (start [component]
    (println "Start Web component." config)
    (let [handler (make-routes-handler {})
          server  (run-jetty {:ring-handler handler
                              :port (get-in config [:web :port] 5050)
                              :join? false
                              :daemon? true})]
      (assoc component :server server)))

  (stop [component]
    (println "Stop Web component.")
    (let [server (:server component)]
      (println 2222 server)
      (.stop server)
      (assoc component :server nil))))

(defn web
  []
  (Web. nil))
