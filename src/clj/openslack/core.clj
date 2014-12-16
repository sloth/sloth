(ns openslack.core
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :refer [chan <!!]]
            [openslack.config :refer [configuration]]
            [openslack.web :refer [web]]
            [openslack.bots :as bots])
  (:gen-class))


(defn make-system
  "OpenSlack system constructor."
  []
  (-> (component/system-map
       :config (configuration)
       :web (web)
       :slothbot (bots/sloth))
      (component/system-using
       {:web [:config]
        :slothbot [:config]})))

(defn initialize
  []
  (component/start (make-system)))

(defn -main
  [& args]
  (let [lock (chan 1)
        sys (initialize)]
    (<!! lock)
    (component/stop sys)))
