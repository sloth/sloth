(ns openslack.core
  (:require [qbits.jet.server :refer [run-jetty]]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [compojure.response :refer [render]]
            [clojure.java.io :as io])
  (:gen-class))

(defn home
  [req]
  (render (io/resource "index.html") req))

;; Routes definition
(defroutes app
  (GET "/" [] home)
  (route/resources "/static")
  (route/not-found "<h1>Page not found</h1>"))

;; Application entry point
(defn -main
  [& args]
  (run-jetty {:ring-handler app :port 5050}))
