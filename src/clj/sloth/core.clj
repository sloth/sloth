(ns sloth.core
  (:require [clojure.java.io :as io]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [compojure.response :refer [render]]
            [qbits.jet.server :refer [run-jetty]])
  (:gen-class))

(defn- home-ctrl
  [request]
  (let [page (io/resource "index.release.html")]
    (render page request)))

(defn- make-routes
  []
  (routes
   (GET "/" [] home-ctrl)
   (route/resources "/static")
   (route/not-found "<h1>Page not found</h1>")))

(defn -main
  [& args]
  (let [handler (make-routes)]
    (run-jetty {:ring-handler handler
                :port 5050
                :join? true})))
