(ns sloth.bots.experiment
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :refer [go <! >! go-loop alts! pub sub unsub close! chan]]
            [sloth.xmpp :as xmpp]))


;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ;; ToyBot (experiment with high level api)
;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; ;; (defn handle-incoming-messages
;; ;;   [conn]
;; ;;   (let [chatm    (xmpp/chat-manager conn)
;; ;;         messages (xmpp/listen-messages chatm)]
;; ;;     (go-loop []
;; ;;       (let [[chat message] (<! messages)]
;; ;;         (println "RECEIVED: " message)
;; ;;         (recur)))))

;; ;; (defn handle-roster-events
;; ;;   [conn]
;; ;;   (let [roster (xmpp/get-roster conn)
;; ;;         events (xmpp/listen-roster roster)]
;; ;;     (go-loop []
;; ;;       (let [[etype data] (<! events)]
;; ;;         (println "roster event" etype ":" data)
;; ;;         (recur)))))

;; ;; (defn handle-incoming-packets
;; ;;   [conn]
;; ;;   (let [packets (xmpp/listen-packets conn)]
;; ;;     (go-loop []
;; ;;       (let [packet (<! packets)]
;; ;;         (println "*******************************")
;; ;;         (println "received packed: " (.getFrom packet) packet)
;; ;;         (println "extension: " (.getExtension packet "urn:xmpp:delay")))
;; ;;       (recur))))

;; (defn initialize-muc
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

;; ;; (defn initialize-service
;; ;;   [conn]
;; ;;   (handle-incoming-messages conn)
;; ;;   (handle-muc conn)
;; ;;   (handle-roster-events conn)
;; ;;   (handle-incoming-packets conn))

;; (defn handle-chat-message
;;   [{:keys [conn packet]}]
;;   (let [msg (doto (org.jivesoftware.smack.packet.Message. (.getFrom packet))
;;               (.setBody "received ;)")
;;               (.setType (.getType packet)))]
;;     (.sendPacket conn msg)))

;; (defn initialize-echo-service
;;   [publication]
;;   (let [subscription (chan)]
;;     (sub publication :message subscription)
;;     (go-loop []
;;       (when-let [message (<! subscription)]
;;         (println "echoservice:" message)
;;         (condp = (:type message)
;;           :chat (handle-chat-message message)
;;           nil))
;;       (recur))))


;; (defrecord ToyXMPPBot [config]
;;   component/Lifecycle
;;   (start [component]
;;     (println "Start toy xmpp bot.")
;;     (let [config (get config :toybot)
;;           conn (xmpp/connection config)
;;           kill (chan)
;;           publisher (chan)
;;           publication (pub publisher #(:topic %))]

;;       (initialize-packet-listener conn publisher kill)
;;       ;; (initialize-echo-service publication)
;;       (initialize-muc conn)

;;       (assoc component
;;         :kill kill
;;         :connection conn
;;         :publication publication)))

;;   (stop [component]
;;     (println "Stop toy xmpp bot.")
;;     (let [{:keys [kill connection]} component]
;;       (close! kill)
;;       (.disconnect connection)
;;       (assoc component
;;         :publication nil
;;         :connection nil))))

;; (defn toy-xmpp-bot
;;   []
;;   (ToyXMPPBot. nil))


;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ;; ToyBot2 (experiment with low level
;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

