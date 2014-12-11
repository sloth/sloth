(ns openslack.config
  (:require [nomad :refer [read-config]]
            [com.stuartsierra.component :as component]
            [clojure.java.io :as io]))

(defn configuration
  "Configuration component."
  []
  (read-config (io/resource "config/main.edn")))
