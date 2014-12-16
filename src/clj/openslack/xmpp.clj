(ns openslack.xmpp
  (:require [clojure.core.async :refer [go put! chan]])
  (:import org.jivesoftware.smack.tcp.XMPPTCPConnection
           org.jivesoftware.smack.ConnectionConfiguration
           org.jivesoftware.smack.filter.PacketFilter
           org.jivesoftware.smack.packet.Message
           org.jivesoftware.smack.packet.Presence
           org.jivesoftware.smack.RosterListener
           org.jivesoftware.smack.PacketListener
           org.jivesoftware.smack.MessageListener
           org.jivesoftware.smack.ChatManager
           org.jivesoftware.smack.ChatManagerListener
           org.jivesoftware.smackx.muc.InvitationListener
           org.jivesoftware.smackx.muc.packet.MUCUser;
           org.jivesoftware.smackx.muc.packet.MUCInitialPresence;
           org.jivesoftware.smackx.muc.MultiUserChat))

(def ^{:doc "Security mode translation map."
       :dynamic true}
  *security-mode*
  {:disabled org.jivesoftware.smack.ConnectionConfiguration$SecurityMode/disabled})

(defn connection
  "Create xmpp connection."
  [{:keys [resource domain username password port login]
    :or {resource "Sloth" port 5222 login true}
    :as opts}]
  (let [conf (ConnectionConfiguration. "144.76.246.114" port domain)]
    (.setSecurityMode conf (get-in *security-mode* [:disabled]))
    (.setHostnameVerifier conf (reify javax.net.ssl.HostnameVerifier
                                 (verify [_ _ _] true)))
    (let [conn (XMPPTCPConnection. conf)]
      (.connect conn)
      (when login
        (.login conn username password resource))
      conn)))

;; (defn chat-manager
;;   "Get chat manager for specified connection."
;;   [conn]
;;   (ChatManager/getInstanceFor conn))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Messages & Chat
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; (defn- make-message-listener
;;   [chatlistener chatmanager channel]
;;   (reify MessageListener
;;     (processMessage [this chat message]
;;       (let [r (put! channel [chat message])]
;;         (when-not (true? r)
;;           (.removeMessageListener chat this)
;;           (.removeChatListener chatmanager chatlistener))))))

;; (defn- make-chat-listener
;;   [chatmanager channel]
;;   (reify ChatManagerListener
;;     (chatCreated [this chat locally?]
;;       (.addMessageListener chat (make-message-listener this chatmanager channel)))))

(defmulti listen-messages (comp class first vector))

;; (defmethod listen-messages ChatManager
;;   ([chatm] (listen-messages chatm (chan)))
;;   ([chatm channel]
;;    (let [chatlistener (make-chat-listener chatm channel)]
;;      (.addChatListener chatm chatlistener)
;;      channel)))

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

(def ^{:doc "Presence type translation map."
       :dynamic true}
  *presence-types*
  {:available org.jivesoftware.smack.packet.Presence$Type/available
   :unavailable org.jivesoftware.smack.packet.Presence$Type/unavailable
   :error org.jivesoftware.smack.packet.Presence$Type/error})

(def ^{:doc "Message type translation map."
       :dynamic true}
  *message-types*
  {:chat org.jivesoftware.smack.packet.Message$Type/chat
   :groupchat org.jivesoftware.smack.packet.Message$Type/groupchat
   :normal org.jivesoftware.smack.packet.Message$Type/normal})

(defn make-presence
  [{:keys [to type]}]
  (let [presence (Presence. (get-in *presence-types* [type]))]
    (when to
      (.setTo presence to))
    presence))

;; (defn multi-user-chat
;;   [conn room]
;;   (MultiUserChat. conn room))

(defn- make-joinroom-packet
  [{:keys [room nickname password]}]
  (let [room (.toLowerCase room)
        to   (str room "/" nickname)
        presence (make-presence {:type :available
                                 :to nickname})
        extension (MUCInitialPresence.)]
    (when password
      (.setPassword extension password))
    (.addExtension presence extension)
    presence))

(defn join-room
  [conn {:keys [room nickname password] :as opts}]
  (let [packet (make-joinroom-packet opts)]
    (.sendPacket conn packet)))

;; (defn listen-muc-invitations
;;   ([conn] (listen-muc-invitations conn (chan)))
;;   ([conn channel]
;;    (let [listener (reify InvitationListener
;;                     (invitationReceived [_ conn room inviter reason password message]
;;                       (let [muc (multi-user-chat conn room)]
;;                         (put! channel {:muc muc :password password}))))]
;;      (MultiUserChat/addInvitationListener conn listener)
;;      channel)))

;; (defmethod listen-messages MultiUserChat
;;   ([muc] (listen-messages muc (chan)))
;;   ([muc channel]
;;    (let [listener (reify PacketListener
;;                     (processPacket [this message]
;;                       (let [r (put! channel [muc message])]
;;                         (when-not (true? r)
;;                           (.removeMessageListener muc this)))))]
;;      (.addMessageListener muc listener)
;;      channel)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Packet listening
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^{:doc "Packet Filter that accepts all packets."}
  packet-filter-all
  (reify PacketFilter
    (accept [_ packet] true)))

;; Filters for groupchat rooms.
;; fromRoomFilter = FromMatchesFilter.create(room);
;; fromRoomGroupchatFilter = new AndFilter(fromRoomFilter, MessageTypeFilter.GROUPCHAT);

;; Invitation filter
;; new AndFilter(PacketTypeFilter.MESSAGE, new PacketExtensionFilter(new MUCUser()),
;;               new NotFilter(MessageTypeFilter.ERROR));

(defn listen-received-packets
  "Add packet listener to the connection and
  return a channel."
  ([conn] (listen-received-packets conn packet-filter-all (chan)))
  ([conn filter] (listen-received-packets conn filter (chan)))
  ([conn filter channel]
   (let [listener (reify PacketListener
                    (processPacket [this packet]
                      (let [r (put! channel packet)]
                        (when-not (true? r)
                          (.removePacketListener conn this)))))]
     (.addPacketListener conn listener filter)
     channel)))

(defn listen-sending-packets
  "Add packet listener to the connection and
  return a channel."
  ([conn] (listen-sending-packets conn packet-filter-all (chan)))
  ([conn filter] (listen-sending-packets conn filter (chan)))
  ([conn filter channel]
   (let [listener (reify PacketListener
                    (processPacket [this packet]
                      (let [r (put! channel packet)]
                        (when-not (true? r)
                          (.removePacketListener conn this)))))]
     (.addPacketSendingListener conn listener filter)
     channel)))
