(ns openslack.xmpp
  (:require [clojure.core.async :refer [go put! chan]]
            [clojure.set :refer [map-invert]]
            [openslack.xmpp.types :as types])
  (:import rocks.xmpp.core.session.TcpConnectionConfiguration
           rocks.xmpp.core.session.XmppSessionConfiguration
           rocks.xmpp.core.session.SessionStatusListener
           rocks.xmpp.core.stanza.model.client.Presence
           rocks.xmpp.core.stanza.model.client.Message
           rocks.xmpp.core.Jid
           rocks.xmpp.core.stanza.MessageListener
           rocks.xmpp.core.stanza.MessageEvent
           rocks.xmpp.debug.gui.VisualDebugger
           rocks.xmpp.core.session.XmppSession))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Constants
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^{:doc "Message type translation map."
       :dynamic true}
  *message-types*
  {:chat rocks.xmpp.core.stanza.model.AbstractMessage$Type/CHAT
   :groupchat rocks.xmpp.core.stanza.model.AbstractMessage$Type/GROUPCHAT
   :headline rocks.xmpp.core.stanza.model.AbstractMessage$Type/HEADLINE
   :error rocks.xmpp.core.stanza.model.AbstractMessage$Type/ERROR
   :normal rocks.xmpp.core.stanza.model.AbstractMessage$Type/NORMAL})

(def ^{:doc "Session status translation map."
       :dynamic true}
  *session-statuses*
  {:initial rocks.xmpp.core.session.XmppSession$Status/INITIAL
   :authenticated rocks.xmpp.core.session.XmppSession$Status/AUTHENTICATED
   :authenticating rocks.xmpp.core.session.XmppSession$Status/AUTHENTICATING
   :closed rocks.xmpp.core.session.XmppSession$Status/CLOSED
   :closing rocks.xmpp.core.session.XmppSession$Status/CLOSING
   :connected rocks.xmpp.core.session.XmppSession$Status/CONNECTED
   :connecting rocks.xmpp.core.session.XmppSession$Status/CONNECTING})


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Stanza transformation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol ToNativeTransformer
  (extern->clj [_] "Convert to openslack type."))

(extend-protocol ToNativeTransformer
  Message
  (extern->clj [message]
    (types/map->Message
     {:type (get (map-invert *message-types*) (.getType message))
      :body (.getBody message)
      :subject (.getSubject message)
      :from (extern->clj (.getFrom message))
      :to   (extern->clj (.getTo message))
      :stanza message}))

  Jid
  (extern->clj [jid]
    (types/map->Jid {:local (.getLocal jid)
                     :domain (.getDomain jid)
                     :resource (.getDomain jid)}))

  rocks.xmpp.core.session.XmppSession$Status
  (extern->clj [status]
    (get (map-invert *session-statuses*) status :unknownstatus))

  Object
  (extern->clj [obj]
    (types/map->Unknown {:data obj})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Session management
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- make-connection-config
  [{:keys [domain] :as cfg}]
  (let [builder (doto (TcpConnectionConfiguration/builder)
                  (.port 5222)
                  (.keepAliveInterval -1)
                  (.hostname domain)
                  (.secure false))]
    (.build builder)))

(defn- make-session-config
  [_]
  (let [builder (XmppSessionConfiguration/builder)]
    (.build builder)))

(defn make-session
  "Create new not connected session."
  [{:keys [domain] :as cfg}]
  (let [connection-cfg (make-connection-config cfg)
        session-cfg (make-session-config cfg)
        connection-cfgs (into-array TcpConnectionConfiguration [connection-cfg])
        session (XmppSession. domain session-cfg connection-cfgs)]
    (.connect session)
    session))

(defn authenticate
  "Send the authentication message to the session."
  [session {:keys [username password resource]
            :or {resource "Sloth"}
            :as cfg}]
  (.login session username password resource))

(defn send-initial-presence
  "Send the initial presence message to jabber server."
  [session]
  (let [presence (Presence.)]
    (.send session presence)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Listeners
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn listen-session-status
  "Lister session statuses and return a channel."
  ([session] (listen-session-status session (chan)))
  ([session channel]
   (let [listener (reify SessionStatusListener
                    (sessionStatusChanged [this event]
                      (let [status (.getStatus event)
                            status (extern->clj status)
                            rsp (put! channel status)]
                        (when-not (true? rsp)
                          (.removeSessionStatusListener session this)))))]
     (.addSessionStatusListener session listener)
     channel)))

(defn listen-messages
  "Listen messages from the session."
  ([session] (listen-messages session (chan)))
  ([session channel]
   (let [listener (reify MessageListener
                    (handle [this event]
                      (let [message (.getMessage event)
                            message (extern->clj message)
                            rsp (put! channel message)]
                        (when-not (true? rsp)
                          (.removeMessageListener session this)))))]
     (.addMessageListener session listener)
     channel)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; MUC
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; (def ^{:doc "Presence type translation map."
;;        :dynamic true}
;;   *presence-types*
;;   {:available org.jivesoftware.smack.packet.Presence$Type/available
;;    :unavailable org.jivesoftware.smack.packet.Presence$Type/unavailable
;;    :error org.jivesoftware.smack.packet.Presence$Type/error})

;; (defn make-presence
;;   [{:keys [to type]}]
;;   (let [presence (Presence. (get *presence-types* type))]
;;     (when to
;;       (.setTo presence to))
;;     presence))

;; (defn make-message
;;   [{:keys [to type]}]
;;   (let [msg (Message. to (get *message-types* type))]
;;     msg))

;; (defn- make-joinroom-packet
;;   [{:keys [room nickname password]}]
;;   (let [room (.toLowerCase room)
;;         to   (str room "@conference.niwi.be" "/" nickname)
;;         presence (make-presence {:type :available
;;                                  :to to})
;;         extension (MUCInitialPresence.)]
;;     (when password
;;       (.setPassword extension password))
;;     (.addExtension presence extension)
;;     presence))

;; (defn multi-user-chat
;;   [conn room]
;;   (let [mucmanager (MultiUserChatManager/getInstanceFor conn)
;;         muc (.getMultiUserChat mucmanager room)]
;;     muc))

;; (defn join-room
;;   [^MultiUserChat muc {:keys [nickname password] :as opts}]
;;   (if (nil? password)
;;     (.join muc nickname)
;;     (.join muc nickname password))))

;; (defn invite-to-room
;;   ([^MultiUserChat muc nickname reason]
;;    (.invite muc nickname reason))
;;   ([^MultiUserChat muc nickname reason password]
;;    (.invite muc nickname reason password)))

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

;; (def ^{:doc "Packet Filter that accepts all packets."}
;;   packet-filter-all
;;   (reify PacketFilter
;;     (accept [_ packet] true)))

;; Filters for groupchat rooms.
;; fromRoomFilter = FromMatchesFilter.create(room);
;; fromRoomGroupchatFilter = new AndFilter(fromRoomFilter, MessageTypeFilter.GROUPCHAT);

;; Invitation filter
;; new AndFilter(PacketTypeFilter.MESSAGE, new PacketExtensionFilter(new MUCUser()),
;;               new NotFilter(MessageTypeFilter.ERROR));




;; (defn listen-received-packets
;;   "Add packet listener to the connection and
;;   return a channel."
;;   ([conn] (listen-received-packets conn packet-filter-all (chan)))
;;   ([conn filter] (listen-received-packets conn filter (chan)))
;;   ([conn filter channel]
;;    (let [listener (reify PacketListener
;;                     (processPacket [this packet]
;;                       (let [r (put! channel packet)]
;;                         (when-not (true? r)
;;                           (.removePacketListener conn this)))))]
;;      (.addPacketListener conn listener filter)
;;      channel)))

;; (defn listen-sending-packets
;;   "Add packet listener to the connection and
;;   return a channel."
;;   ([conn] (listen-sending-packets conn packet-filter-all (chan)))
;;   ([conn filter] (listen-sending-packets conn filter (chan)))
;;   ([conn filter channel]
;;    (let [listener (reify PacketListener
;;                     (processPacket [this packet]
;;                       (let [r (put! channel packet)]
;;                         (when-not (true? r)
;;                           (.removePacketListener conn this)))))]
;;      (.addPacketSendingListener conn listener filter)
;;      channel)))
