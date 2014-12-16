(ns openslack.bots.sloth
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :refer [go <! >! go-loop alts! pub sub unsub close! chan put!]]
            [clojure.string :as str]
            [clojure.set :refer [map-invert]]
            [openslack.xmpp :as xmpp]
            [buddy.sign.generic :as sign])
  (:import rocks.xmpp.core.session.SessionStatusListener))

(defn start-session-status-watcher
  [session kill]
  (let [statuses (xmpp/listen-session-status session)]
    (go-loop []
      (let [[event c] (alts! [kill statuses])]
        (cond
          (= kill c)
          (close! statuses)

          :else
          (when-not (nil? event)
            (println "STATUS: " event)
            (recur)))))))

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
  [session kill]
  (let [messages (xmpp/listen-messages session)]
    (go-loop []
      (let [[message c] (alts! [kill messages])]
        (cond
          (= kill c)
          (close! messages)

          :else
          (when-not (nil? message)
            (println "*start******************************")
            (println "received message:" message)
            (println "*end******************************")
            (recur)))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Component definition
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrecord Sloth [config]
  component/Lifecycle
  (start [component]
    (println "Start sloth.")
    (let [config (get-in config [:bots :sloth])
          session (xmpp/make-session config)
          kill (chan)
          publisher (chan)
          publication (pub publisher #(:topic %))]

      (start-session-status-watcher session kill)

      (xmpp/authenticate session config)
      (xmpp/send-initial-presence session)

      ;; Sniffers
      (start-messages-watcher session kill)

      ;; Internal services routines
      ;; (initialize-messages-watcher publication)

      (assoc component
        :kill kill
        :session session
        :publication publication)))

  (stop [component]
    (println "Stop sloth")
    (let [{:keys [kill session]} component]
      (close! kill)
      (.close session)
      (assoc component
        :publication nil
        :session nil))))

(defn sloth
  []
  (Sloth. nil))
