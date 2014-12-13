(ns openslack.xmpp
  (:require [clojure.core.async :refer [go put! chan]])
  (:import org.jivesoftware.smack.tcp.XMPPTCPConnection
           org.jivesoftware.smack.ConnectionConfiguration
           org.jivesoftware.smack.filter.PacketFilter
           org.jivesoftware.smack.RosterListener
           org.jivesoftware.smack.PacketListener
           org.jivesoftware.smack.MessageListener
           org.jivesoftware.smack.ChatManager
           org.jivesoftware.smack.ChatManagerListener
           org.jivesoftware.smackx.muc.InvitationListener
           org.jivesoftware.smackx.muc.MultiUserChat))

(defn connection
  "Create xmpp connection."
  [{:keys [resource domain username password port]
    :or {resource "Sloth" port 5222}
    :as opts}]
  (let [conf (doto (ConnectionConfiguration. "144.76.246.114" port domain)
               (.setHostnameVerifier (reify javax.net.ssl.HostnameVerifier
                                       (verify [_ _ _] true)))
               (.setSecurityMode (org.jivesoftware.smack.ConnectionConfiguration$SecurityMode/disabled)))
        conn (XMPPTCPConnection. conf)]
    (.connect conn)
    (.login conn username password resource)
    conn))

(defn chat-manager
  "Get chat manager for specified connection."
  [conn]
  (ChatManager/getInstanceFor conn))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Messages & Chat
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- make-message-listener
  [chatlistener chatmanager channel]
  (reify MessageListener
    (processMessage [this chat message]
      (let [r (put! channel [chat message])]
        (when-not (true? r)
          (.removeMessageListener chat this)
          (.removeChatListener chatmanager chatlistener))))))

(defn- make-chat-listener
  [chatmanager channel]
  (reify ChatManagerListener
    (chatCreated [this chat locally?]
      (.addMessageListener chat (make-message-listener this chatmanager channel)))))

(defmulti listen-messages (comp class first vector))

(defmethod listen-messages ChatManager
  ([chatm] (listen-messages chatm (chan)))
  ([chatm channel]
   (let [chatlistener (make-chat-listener chatm channel)]
     (.addChatListener chatm chatlistener)
     channel)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Roster
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Set default subscription mode
(org.jivesoftware.smack.Roster/setDefaultSubscriptionMode
 (org.jivesoftware.smack.Roster$SubscriptionMode/accept_all))

(defn get-roster
  [conn]
  (.getRoster conn))

;; TODO: resource management
(defn listen-roster
  ([roster] (listen-roster roster (chan)))
  ([roster channel]
   (let [listener (reify RosterListener
                    (entriesAdded [_ addresses]
                      (put! channel [:entries-added addresses]))
                    (entriesDeleted [_ addresses]
                      (put! channel [:entries-deleted addresses]))
                    (entriesUpdated [_ addresses]
                      (put! channel [:entries-updated addresses]))
                    (presenceChanged [_ presence]
                      (put! channel [:presence presence])))]
     (.addRosterListener roster listener)
     channel)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; MUC
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn multi-user-chat
  [conn room]
  (MultiUserChat. conn room))

(defn join!
  [muc nickname password]
  (.join muc nickname password))

(defn listen-muc-invitations
  ([conn] (listen-muc-invitations conn (chan)))
  ([conn channel]
   (let [listener (reify InvitationListener
                    (invitationReceived [_ conn room inviter reason password message]
                      (let [muc (multi-user-chat conn room)]
                        (put! channel {:muc muc :password password}))))]
     (MultiUserChat/addInvitationListener conn listener)
     channel)))

(defmethod listen-messages MultiUserChat
  ([muc] (listen-messages muc (chan)))
  ([muc channel]
   (let [listener (reify PacketListener
                    (processPacket [this message]
                      (let [r (put! channel [muc message])]
                        (when-not (true? r)
                          (.removeMessageListener muc this)))))]
     (.addMessageListener muc listener)
     channel)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Packet listening
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn packet-filter
  "Create a packet filter from callable."
  [callable]
  (reify PacketFilter
    (accept [_ packet]
      (callable packet))))

(def ^{:doc "Packet Filter that accepts all packets."}
  packet-filter-all
  (reify PacketFilter
    (accept [_ packet] true)))

(defn listen-packets
  "Add packet listener to the connection and
  return a channel."
  ([conn] (listen-packets conn packet-filter-all (chan)))
  ([conn filter] (listen-packets conn filter (chan)))
  ([conn filter channel]
   (let [listener (reify PacketListener
                    (processPacket [this packet]
                      (let [r (put! channel packet)]
                        (when-not (true? r)
                          (.removePacketListener this)))))]
     (.addPacketListener conn listener filter)
     channel)))
