(ns sloth.core
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :refer [chan <!!]]
            [sloth.config :refer [configuration]]
            [sloth.web :refer [web]]
            [sloth.bots :as bots])
  (:gen-class))

(defn make-system
  "Sloth system constructor."
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
