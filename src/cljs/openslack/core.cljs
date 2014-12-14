(ns openslack.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [cats.core :refer [mlet-with]])
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :refer [<!]]
            [secretary.core :as secretary :refer-macros [defroute]]
            [weasel.repl :as ws-repl]
            [openslack.state :as st]
            [openslack.events :as events]
            [openslack.xmpp :as xmpp]
            [openslack.async :as async]
            [cats.core :as m]
            [cats.monad.either :as either])
  (:import goog.History))

;; Enable println

(enable-console-print!)

;; App state

(def state (atom (st/make-initial-state)))

(add-watch state :log (fn [_ _ _ newval]
                        (print newval)))

;; XMPP

(def xmpp-config {:jid "homer@niwi.be"
                  :password "donuts"
                  :transports ["bosh"]
                  :boshURL "http://niwi.be:5280/http-bind"})
(def client (xmpp/create-client xmpp-config))

(set! js/window.cl client)
(swap! state assoc :client client)

;; Start XMPP session

(go
  (let [mv (<! (mlet-with async/either-pipeline-monad
                [jid (xmpp/start-session client)
                 roster (xmpp/get-roster client)]
                (m/return {:jid jid, :roster roster})))]
    (when (either/right? mv)
      (xmpp/send-presence client)
      (let [{:keys [jid roster]} (either/from-either mv)]
        (swap! state assoc :jid jid)
        (swap! state assoc :roster roster)))))

; TODO: roster-updating process
(def roster-updates-chan (xmpp/roster-updates client))
(go (loop []
      (let [rupdate (<! roster-updates-chan)
            subscription (:subscription rupdate)
            jid (:jid rupdate)
            bare (:bare jid)]
        (case (:subscription rupdate)
          :none :none ; TODO: fill this in
          :both :both
          :to   :to
          :from :from))
      (recur)))

; Chat updating process
(def chats-chan (xmpp/chats client))
(go-loop [chat (<! chats-chan)]
 (swap! state st/add-chat chat)
 (recur (<! chats-chan)))

; TODO: chat-states updating process
(def chat-states-chan (xmpp/chat-states client))
(go (loop []
      (let [chat-state (<! chat-states-chan)]
        (println "chat-state: " chat-state))
      (recur)))

; TODO: subscription-managing process
(def subscriptions-chan (xmpp/subscriptions client))
(def unsubscriptions-chan (xmpp/unsubscriptions client))

;; Enable browser enabled repl.
;; (ws-repl/connect "ws://localhost:9001")

(defn main
  []
  (let [history (History.)]
    (go
      (let [event (<! (events/listen history "navigate"))]
        (secretary/dispatch! (.-token event))))
    (.setEnabled history true)))

(main)
