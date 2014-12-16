(ns openslack.bots.sloth
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :refer [go <! >! go-loop alts! pub sub unsub close! chan put!]]
            [clojure.string :as str]
            [clojure.set :refer [map-invert]]
            [openslack.xmpp :as xmpp]
            [buddy.sign.generic :as sign])
  (:import org.jivesoftware.smack.packet.Message
           org.jivesoftware.smack.packet.Registration))


(defmulti transform-packet class)

(defmethod transform-packet org.jivesoftware.smack.packet.Message
  [packet]
  (let [mtypes (map-invert xmpp/*message-types*)
        type (get mtypes (.getType packet) :unknown)]
    {:topic :message
     :type type
     :packet packet}))

(defmethod transform-packet org.jivesoftware.smack.packet.Presence
  [packet]
  (let [ptypes (map-invert xmpp/*presence-types*)
        type (get ptypes (.getType packet) :unknown)]
    {:topic :presence
     :type type
     :packet packet}))

(defmethod transform-packet :default
  [packet]
  {:topic :unknown
   :packet packet})

(defn initialize-packet-sending-watcher
  "Goroutine for tracing sended packets."
  [conn publisher kill]
  (let [packets (xmpp/listen-sending-packets conn)]
    (go-loop []
      (let [[packet c] (alts! [kill packets])]
        (cond
          (= kill c)
          (close! packets)

          :else
          (when-not (nil? packet)
            (println "=start===========================")
            (println "seinded packed: " packet)
            (println "=start===========================")
            (recur)))))))

(defn initialize-packet-receiving-watcher
  "Goroutine for tracing received packets."
  [conn publisher kill]
  (let [packets (xmpp/listen-received-packets conn)]
    (go-loop []
      (let [[packet c] (alts! [kill packets])]
        (cond
          (= kill c)
          (close! packets)

          :else
          (when-not (nil? packet)
            (let [packet (transform-packet packet)]
              (println "*start******************************")
              (println "received packed: " packet)
              (println "*end******************************")
              (put! publisher (assoc packet :conn conn))
              (recur))))))))

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

(defn command-help
  [conn packet params]
  (let [commands (map name (keys *commands*))
        msg (doto (Message. (.getFrom packet))
              (.setBody (apply str "Command list: " (interpose ", " commands)))
              (.setType (.getType packet)))]
    (.sendPacket conn msg)))

(def ^{:dynamic true
       :doc "Map that defines a lookup betwen command name and its implementation."}
  *commands*
  {:help command-help})

(defn process-chat-packet
  "Function that handles personal messages to
  the bot. That messages will to be interpreted
  like commands."
  [conn packet]
  ;; TODO: implement more robust command parsing.
  (when-let [body (.getBody packet)]
    (let [[command & rest] (str/split body #"\s+")
          cmd (get *commands* (keyword command) command-help)]
      (cmd conn packet rest))))

(defn process-groupchat-packet
  [conn packet]
  ;; do nothing at this moment
  )

(defn initialize-messages-watcher
  [publication]
  (let [subscriber (chan)]
    (sub publication :message subscriber)
    (go-loop []
      (when-let [received (<! subscriber)]
        (let [{:keys [type conn packet]} received]
          (condp = type
            :chat (process-chat-packet conn packet)
            :groupchat (process-groupchat-packet conn packet)
            nil)
          (recur))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Roster watcher
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO: not works properly. implement using low level api
;; currently only used for autoaccept user contacts.
(defn initialize-roster-watcher
  [conn kill]
  (let [roster (xmpp/get-roster conn)
        events (xmpp/listen-roster roster)]
    (go-loop []
      (when-let [event (<! events)]
        (let [[type data] event]
          (println type data)
          (recur))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Component definition
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrecord Sloth [config]
  component/Lifecycle
  (start [component]
    (println "Start sloth.")
    (let [config (get-in config [:bots :sloth])
          conn (xmpp/connection config)
          kill (chan)
          publisher (chan)
          publication (pub publisher #(:topic %))]

      ;; Sniffers
      (initialize-packet-receiving-watcher conn publisher kill)
      (initialize-packet-sending-watcher conn publisher kill)

      ;; Internal services routines
      (initialize-roster-watcher conn kill)
      (initialize-messages-watcher publication)

      (assoc component
        :kill kill
        :connection conn
        :publication publication)))

  (stop [component]
    (println "Stop sloth")
    (let [{:keys [kill connection]} component]
      (close! kill)
      (.disconnect connection)
      (assoc component
        :publication nil
        :connection nil))))

(defn sloth
  []
  (Sloth. nil))
