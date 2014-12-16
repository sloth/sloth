(ns openslack.bots.sloth
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :refer [go <! >! go-loop alts! pub sub unsub close! chan put!]]
            [clojure.string :as str]
            [clojure.set :refer [map-invert]]
            [openslack.xmpp :as xmpp]
            [buddy.sign.generic :as sign])
  (:import rocks.xmpp.core.session.SessionStatusListener))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Session status watcher
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn start-session-status-watcher
  [session]
  (let [statuses (xmpp/listen-session-status session)]
    (go-loop []
      (when-let [status (<! statuses)]
        (println "STATUS: " status)
        (recur)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Commands
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(declare ^:dynamic  *commands*)

;; (defn command-auth
;;   [conn packet params config]
;;   (let [from  (first (str/split (.getFrom packet) #"/"))
;;         token (sign/dumps {:jid from} *secret*)
;;         msg   (message token from (.getType packet))]
;;     (println from)
;;     (println token)
;;     (.sendPacket conn msg)))

;; (defn command-register
;;   [conn packet params config]
;;   (let [config (assoc config :login false)
;;         conn   (xmpp/connection config)]
;;     (let [[username password] params
;;           registration (doto (Registration.)
;;                          (.setType org.jivesoftware.smack.packet.IQ$Type/SET)
;;                          (.setAttributes (java.util.HashMap. {"email" "niwi@niwi.be"
;;                                                               "username" username
;;                                                               "password" password})))]
;;       (.sendPacket conn registration))))

;; (defn command-help
;;   [conn packet params]
;;   (let [commands (map name (keys *commands*))
;;         msg (doto (Message. (.getFrom packet))
;;               (.setBody (apply str "Command list: " (interpose ", " commands)))
;;               (.setType (.getType packet)))]
;;     (.sendPacket conn msg)))

;; (defn command-create-room
;;   [conn packet roomname]
;;   (let [roomname (str roomname "@conference.niwi.be")
;;         muc (xmpp/multi-user-chat chat roomname)]
;;     (xmpp/join-room muc {:nickname "sloth"})
;;     (xmpp/invite-to-room muc "miguel" "Por que puedo")))

;; (def ^{:dynamic true
;;        :doc "Map that defines a lookup betwen command name and its implementation."}
;;   *commands*
;;   {:help command-help
;;    :create-room command-create-room})

;; (defn process-chat-packet
;;   "Function that handles personal messages to
;;   the bot. That messages will to be interpreted
;;   like commands."
;;   [conn packet]
;;   ;; TODO: implement more robust command parsing.
;;   (when-let [body (.getBody packet)]
;;     (let [[command rest] (str/split body #"\s+" 2)
;;           _ (println 222 command)
;;           _ (println 222 rest)
;;           cmd (get *commands* (keyword command) command-help)]
;;       (cmd conn packet rest))))

;; (defn process-groupchat-packet
;;   [conn packet]
;;   ;; do nothing at this moment
;;   )

;; (defn initialize-messages-watcher
;;   [publication]
;;   (let [subscriber (chan)]
;;     (sub publication :message subscriber)
;;     (go-loop []
;;       (when-let [received (<! subscriber)]
;;         (let [{:keys [type conn packet]} received]
;;           (condp = type
;;             :chat (process-chat-packet conn packet)
;;             :groupchat (process-groupchat-packet conn packet)
;;             nil)
;;           (recur))))))

(defn start-messages-watcher
  [session]
  (let [messages (xmpp/listen-messages session)]
    (go-loop []
      (when-let [message (<! messages)]
        (println "*start******************************")
        (println "received message:" message)
        (println "*end******************************")
        (recur)))))

(defn start-presence-watcher
  [session]
  (let [presences (xmpp/listen-presence session)]
    (go-loop []
      (when-let [presence (<! presences)]
        (println "received presence:" presence)
        (recur)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Component definition
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrecord Sloth [config]
  component/Lifecycle
  (start [component]
    (println "Start sloth.")
    (let [config (get-in config [:bots :sloth])
          session (xmpp/make-session config)]

      (start-session-status-watcher session)
      (xmpp/authenticate session config)
      (xmpp/send-initial-presence session)

      ;; Sniffers
      (start-messages-watcher session)
      (start-presence-watcher session)

      ;; Internal services routines
      ;; (initialize-messages-watcher publication)

      (assoc component
        :session session)))

  (stop [component]
    (println "Stop sloth")
    (let [{:keys [session]} component]
      (.close session)
      (assoc component
        :session nil))))

(defn sloth
  []
  (Sloth. nil))
