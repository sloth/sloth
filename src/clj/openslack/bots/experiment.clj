(ns openslack.bots.experiment
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :refer [go <! go-loop alts!]]
            [openslack.xmpp :as xmpp]))

(defn handle-incoming-messages
  [conn]
  (let [chatm    (xmpp/chat-manager conn)
        messages (xmpp/listen-messages chatm)]
    (go-loop []
      (let [[chat message] (<! messages)]
        (println "RECEIVED: " message)
        (recur)))))

(defn handle-roster-events
  [conn]
  (let [roster (xmpp/get-roster conn)
        events (xmpp/listen-roster roster)]
    (go-loop []
      (let [[etype data] (<! events)]
        (println "roster event" etype ":" data)
        (recur)))))

(defn handle-incoming-packets
  [conn]
  (let [packets (xmpp/listen-packets conn)]
    (go-loop []
      (let [packet (<! packets)]
        (println "*******************************")
        (println "received packed: " (.getFrom packet) packet)
        (println "extension: " (.getExtension packet "urn:xmpp:delay")))
      (recur))))

(defn handle-muc
  [conn]
  (let [invitations (xmpp/listen-muc-invitations conn)]
    (go-loop [chans [invitations]]
      (let [[v c] (alts! chans)]
        (condp = c
          invitations
          (let [{:keys [muc password]} v]
            (println "Invitation" muc (.getRoom muc))
            (xmpp/join! muc "toybot" password)
            (recur (conj chans (xmpp/listen-messages muc))))

          (let [[chat message] v]
            ;; (println "muc message" message chat)
            (recur chans)))))))

(defn initialize-service
  [conn]
  (handle-incoming-messages conn)
  (handle-muc conn)
  (handle-roster-events conn)
  (handle-incoming-packets conn))

(defrecord ToyXMPPBot [config]
  component/Lifecycle
  (start [component]
    (println "Start toy xmpp bot.")
    (let [config (get config :toybot)
          conn   (xmpp/connection config)]
      (initialize-service conn)
      (assoc component :connection conn)))

  (stop [component]
    (println "Stop toy xmpp bot.")
    (let [conn (:connection component)]
      (.disconnect conn)
      (assoc component :connection nil))))

(defn toy-xmpp-bot
  []
  (ToyXMPPBot. nil))
