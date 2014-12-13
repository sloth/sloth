(ns openslack.bots.experiment
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :refer [go <! >! go-loop alts! pub sub unsub close! chan]]
            [openslack.xmpp :as xmpp]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ToyBot (experiment with high level api)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;; (defn handle-incoming-messages
;;   [conn]
;;   (let [chatm    (xmpp/chat-manager conn)
;;         messages (xmpp/listen-messages chatm)]
;;     (go-loop []
;;       (let [[chat message] (<! messages)]
;;         (println "RECEIVED: " message)
;;         (recur)))))

;; (defn handle-roster-events
;;   [conn]
;;   (let [roster (xmpp/get-roster conn)
;;         events (xmpp/listen-roster roster)]
;;     (go-loop []
;;       (let [[etype data] (<! events)]
;;         (println "roster event" etype ":" data)
;;         (recur)))))

;; (defn handle-incoming-packets
;;   [conn]
;;   (let [packets (xmpp/listen-packets conn)]
;;     (go-loop []
;;       (let [packet (<! packets)]
;;         (println "*******************************")
;;         (println "received packed: " (.getFrom packet) packet)
;;         (println "extension: " (.getExtension packet "urn:xmpp:delay")))
;;       (recur))))

;; (defn handle-muc
;;   [conn]
;;   (let [invitations (xmpp/listen-muc-invitations conn)]
;;     (go-loop [chans [invitations]]
;;       (let [[v c] (alts! chans)]
;;         (condp = c
;;           invitations
;;           (let [{:keys [muc password]} v]
;;             (println "Invitation" muc (.getRoom muc))
;;             (xmpp/join! muc "toybot" password)
;;             (recur (conj chans (xmpp/listen-messages muc))))

;;           (let [[chat message] v]
;;             ;; (println "muc message" message chat)
;;             (recur chans)))))))

;; (defn initialize-service
;;   [conn]
;;   (handle-incoming-messages conn)
;;   (handle-muc conn)
;;   (handle-roster-events conn)
;;   (handle-incoming-packets conn))

(defmulti transform-packet class)
(defmethod transform-packet org.jivesoftware.smack.packet.Message
  [packet]
  (let [type (condp = (.getType packet)
               org.jivesoftware.smack.packet.Message$Type/chat :chat
               org.jivesoftware.smack.packet.Message$Type/groupchat :groupchat
               org.jivesoftware.smack.packet.Message$Type/normal :normal
               :unknown)]
    {:topic :message
     :type type
     :packet packet}))

(defmethod transform-packet org.jivesoftware.smack.packet.Presence
  [packet]
  (let [type (condp = (.getType packet)
               org.jivesoftware.smack.packet.Presence$Type/available :available
               org.jivesoftware.smack.packet.Presence$Type/unavailable :unavailable
               org.jivesoftware.smack.packet.Presence$Type/error :error
               :unknown)]
    {:topic :presence
     :type type
     :packet packet}))

(defmethod transform-packet :default
  [packet]
  {:topic :unexpected
   :packet packet})

(defn handle-chat-message
  [{:keys [conn packet]}]
  (let [msg (doto (org.jivesoftware.smack.packet.Message. (.getFrom packet))
              (.setBody "received ;)")
              (.setType (.getType packet)))]
    (.sendPacket conn msg)))

(defn initialize-echo-service
  [publication]
  (let [subscription (chan)]
    (sub publication :message subscription)
    (go-loop []
      (when-let [message (<! subscription)]
        (println "echoservice:" message)
        (condp = (:type message)
          :chat (handle-chat-message message)
          nil))
      (recur))))

(defn initialize-packet-listener
  [conn publisher kill]
  (let [packets (xmpp/listen-packets conn)]
    (go-loop []
      (let [[packet c] (alts! [kill packets])]
        (cond
          (= kill c)
          (close! packets)

          (= packets c)
          (when-not (nil? packet)
            (let [packet (transform-packet packet)]
              (println "*******************************")
              (println "received packed: " packet)
              ;; (println "extension: " (.getExtension packet "urn:xmpp:delay"))
              (println "*******************************")
              (>! publisher (assoc packet :conn conn))))))
      (recur))))



(defrecord ToyXMPPBot [config]
  component/Lifecycle
  (start [component]
    (println "Start toy xmpp bot.")
    (let [config (get config :toybot)
          conn (xmpp/connection config)
          kill (chan)
          publisher (chan)
          publication (pub publisher #(:topic %))]
      (initialize-packet-listener conn publisher kill)
      (initialize-echo-service publication)
      (assoc component
        :kill kill
        :connection conn
        :publicatin publication)))

  (stop [component]
    (println "Stop toy xmpp bot.")
    (let [{:keys [kill connection]} component]
      (close! kill)
      (.disconnect connection)
      (assoc component
        :publication nil
        :connection nil))))

(defn toy-xmpp-bot
  []
  (ToyXMPPBot. nil))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ToyBot2 (experiment with low level
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

