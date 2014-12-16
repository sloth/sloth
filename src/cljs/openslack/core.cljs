(ns openslack.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :refer [<!]]
            [weasel.repl :as ws-repl]
            [openslack.config :as config]
            [openslack.routing :refer [start-history!]]
            [openslack.state :as st]
            [openslack.xmpp :as xmpp]
            [openslack.views :as views]
            [cats.core :as m]
            [cats.monad.either :as either]))

(enable-console-print!)

;; Enable browser enabled repl.
(ws-repl/connect "ws://localhost:9001")

(defn start-processes!
  []
  nil)

(defn start-xmpp-session!
  []
  (let [client (:client @st/state)]
    ; Raw events (debug)
    (.on client "raw:*" (fn [ev payload]
                          (.log js/console ev payload)))

    ; Presence
    (xmpp/send-presence client)

    ; Roster
    (go-loop [mroster (<! (xmpp/get-roster client))]
      (if-let [roster (either/from-either mroster)]
        (swap! st/state assoc :roster roster)
        (recur (<! (xmpp/get-roster client)))))

    ; Contact presences
    (let [presence-chan (xmpp/presences client)]
      (go-loop [presence (<! presence-chan)]
        (swap! st/state st/update-presence presence)
        (recur (<! presence-chan))))

    ; Subscriptions
;    (let [subs (xmpp/subscriptions client)]
;      (go-loop [s (<! subs)]
;        (recur (<! subs))))

    ; Rooms
    (go
      (let [room (<! (xmpp/join-room client "sloth@conference.niwi.be" (:local (:user @st/state))))]
        ; TODO: join my room
        ))

    ; Chat updating process
    (let [chats (xmpp/chats client)]
      (go-loop [chat (<! chats)]
        (swap! st/state st/add-chat chat)
        (recur (<! chats))))
))

(defn render-view!
  []
  (om/root views/app st/state {:target (js/document.querySelector "body")}))

(defn watch-login!
  []
  (add-watch st/state :log-in (fn [_ _ oldval newval]
                                (when (and (not (st/logged-in? oldval))
                                           (st/logged-in? newval))
                                    (start-xmpp-session!)))))

(defn main
  []
  (start-history!)
  (watch-login!)
  (render-view!)
)


(main)
