(ns user
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.namespace.repl :refer (refresh)]
            [weasel.repl.websocket :refer [repl-env]]
            [cemerick.piggieback :refer [cljs-repl]]
            [openslack.core :refer [initialize]]))


(def system nil)

(defn brepl
  "Start the browser repl."
  []
  (cljs-repl :repl-env (repl-env :ip "0.0.0.0" :port 9001)))

(defn start
  []
  (alter-var-root #'system
    (constantly (initialize))))

(defn stop
  []
  (alter-var-root #'system
    (fn [s] (when s (component/stop s)))))

(defn reset
  []
  (stop)
  (refresh :after 'user/start))
