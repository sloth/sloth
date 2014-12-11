(ns openslack.core
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :refer [chan <!!]]
            [openslack.config :refer [configuration]]
            [openslack.web :refer [web]])
  (:gen-class))


(defn make-system
  "OpenSlack system constructor."
  []
  (-> (component/system-map
       :config (configuration)
       :web (web))
      (component/system-using
       {:web [:config]})))

(defn initialize
  []
  (component/start (make-system)))

(defn -main
  [& args]
  (let [lock (chan 1)]
    (initialize)
    (<!! lock)))
