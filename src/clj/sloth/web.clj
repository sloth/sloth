(ns sloth.web
  (:require [com.stuartsierra.component :as component]
            [qbits.jet.server :refer [run-jetty]]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [compojure.response :refer [render]]
            [sloth.web.home :as home]
            [sloth.logging :as logging]))

(defn- make-routes-handler
  [config]
  (routes
   (GET "/" [] home/home-ctrl)
   (route/resources "/static")
   (route/not-found "<h1>Page not found</h1>")))

(defrecord Web [config]
  component/Lifecycle
  (start [component]
    (logging/info "Start web component." config)
    (println "Start Web component." config)

    (let [handler (make-routes-handler {})
          server  (run-jetty {:ring-handler handler
                              :port (get-in config [:web :port] 5050)
                              :join? false
                              :daemon? true})]
      (assoc component :server server)))

  (stop [component]
    (logging/info "Stop web component.")
    (let [server (:server component)]
      (.stop server)
      (assoc component :server nil))))

(defn web
  []
  (Web. nil))
