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

;; XMPP

;; Start XMPP session

;(go
;  (let [mv (<! (with-monad async/either-pipeline-monad
;                 (mlet [user (xmpp/start-session client)
;                        roster (xmpp/get-roster client)
;                        room (xmpp/join-room client "testroom@conference.niwi.be" "kim")]
;                   (m/return {:user user, :roster roster, :room room}))))]
;    (when (either/right? mv)
;      (xmpp/send-presence client)
;      (xmpp/update-capabilities client)
;      (let [{:keys [user roster room]} (either/from-either mv)]
;        (swap! st/state assoc :user user)
;        (swap! st/state assoc :roster roster)
;        (swap! st/state assoc :features (xmpp/get-features client))
;        (swap! st/state st/join-room room)))))

; TODO: roster-updating process
;(def roster-updates-chan (xmpp/roster-updates client))
;(go (loop []
;      (let [rupdate (<! roster-updates-chan)
;            subscription (:subscription rupdate)
;            jid (:jid rupdate)
;            bare (:bare jid)]
;        (case (:subscription rupdate)
;          :none :none ; TODO: fill this in
;          :both :both
;          :to   :to
;          :from :from))
;      (recur)))

; Chat updating process
;(def chats-chan (xmpp/chats client))
;(go-loop [chat (<! chats-chan)]
; (swap! st/state st/add-chat chat)
; (recur (<! chats-chan)))

; TODO: chat-states updating process
;(def chat-states-chan (xmpp/chat-states client))
;(go-loop [chat-state (<! chat-states-chan)]
;  (println "chat-state: " chat-state)
;  (recur (<! chat-states-chan)))


; TODO: subscription-managing process
;(def subscriptions-chan (xmpp/subscriptions client))
;(def unsubscriptions-chan (xmpp/unsubscriptions client))
;
;;; Enable browser enabled repl.
;; (ws-repl/connect "ws://localhost:9001")

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

    ; Contact presences
    (let [presence-chan (xmpp/presences client)]
      (go-loop [presence (<! presence-chan)]
        (.log js/console ":OLLLL" (pr-str presence))
        (swap! st/state st/update-presence presence)
        (recur (<! presence-chan))))


    ; Roster
    (go-loop [mroster (<! (xmpp/get-roster client))]
      (if-let [roster (either/from-either mroster)]
        (swap! st/state assoc :roster roster)
        (recur (<! (xmpp/get-roster client)))))

    ; Subscriptions
;    (let [subs (xmpp/subscriptions client)]
;      (go-loop [s (<! subs)]
;        (recur (<! subs))))
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
