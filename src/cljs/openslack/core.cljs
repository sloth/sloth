(ns openslack.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :refer [<!]]
            [secretary.core :as secretary :refer-macros [defroute]]
            [weasel.repl :as ws-repl]
            [openslack.utils :as utils]
            [openslack.xmpp :as xmpp]
            [cats.monad.either :as either])
  (:import goog.History))

;; Enable println
(enable-console-print!)

(def xmpp-config {:jid "dialelo@niwi.be"
                  :password "dragon"
                  :transports ["bosh"]
                  :boshURL "http://niwi.be:5280/http-bind"})
(def client (xmpp/create-client xmpp-config))

(go
  (let [jid (<! (xmpp/start-session client))
        roster (<! (xmpp/get-roster client))]
    (when (every? either/right? [jid roster])
      (print "Connected as: " (either/from-either jid))
      (print "roster: " (either/from-either roster)))))

;; Enable browser enabled repl.
;; (ws-repl/connect "ws://localhost:9001")

(defn main
  []
  (let [history (History.)]
    (go
      (let [event (<! (utils/listen history "navigate"))]
        (secretary/dispatch! (.-token event))))
    (.setEnabled history true)))

(main)
