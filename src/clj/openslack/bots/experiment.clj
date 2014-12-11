(ns openslack.bots.experiment
  (:require [com.stuartsierra.component :as component])
  (:import org.jivesoftware.smack.tcp.XMPPTCPConnection
           org.jivesoftware.smack.MessageListener
           org.jivesoftware.smack.ChatManager
           org.jivesoftware.smack.ChatManagerListener))


(defn- make-connection
  []
  (let [conn (XMPPTCPConnection. "localhost")]
    (.connect conn)
    (.login conn "testuser" "123123", "Bot")
    conn))


(defn make-message-listener
  []
  (reify MessageListener
    (processMessage [this chat message]
      (println "RECEIVED:" message)
      (.sendMessage chat message))))

(defn make-chat-listener
  []
  (reify ChatManagerListener
    (chatCreated [_ chat locally?]
      (println "Chat created" chat)
      (when-not locally?
        (.addMessageListener chat (make-message-listener))))))


(defrecord ToyXMPPBot [config]
  component/Lifecycle
  (start [component]
    (println "Start toy xmpp bot.")
    (let [conn (make-connection)
          chatmanager (ChatManager/getInstanceFor conn)]
      (.addChatListener chatmanager (make-chat-listener))
      (assoc component :connection conn)))

  (stop [component]
    (println "Stop toy xmpp bot.")
    (let [conn (:connection component)]
      (.disconnect conn)
      (assoc component :connection nil))))

(defn toy-xmpp-bot
  []
  (ToyXMPPBot. nil))
