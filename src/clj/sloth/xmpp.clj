(ns sloth.xmpp
  (:require [clojure.core.async :refer [go put! chan <! close!]]
            [clojure.set :refer [map-invert]]
            [sloth.xmpp.types :as types]
            [sloth.logging :as logging])
  (:import rocks.xmpp.core.session.TcpConnectionConfiguration
           rocks.xmpp.core.session.XmppSessionConfiguration
           rocks.xmpp.core.session.SessionStatusListener
           rocks.xmpp.core.stanza.model.client.Presence
           rocks.xmpp.core.stanza.model.client.Message
           rocks.xmpp.core.Jid
           rocks.xmpp.core.stanza.MessageEvent
           rocks.xmpp.core.stanza.PresenceEvent
           rocks.xmpp.core.stanza.MessageListener
           rocks.xmpp.core.stanza.PresenceListener
           rocks.xmpp.core.stanza.MessageEvent
           rocks.xmpp.debug.gui.VisualDebugger
           rocks.xmpp.core.session.XmppSession
           rocks.xmpp.extensions.muc.MultiUserChatManager
           rocks.xmpp.extensions.ping.PingManager
           rocks.xmpp.extensions.muc.InvitationListener
           org.apache.commons.lang3.StringUtils))

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

(def ^{:doc "Session status translation map."
       :dynamic true}
  *presence-types*
  {:error rocks.xmpp.core.stanza.model.AbstractPresence$Type/ERROR
   :probe rocks.xmpp.core.stanza.model.AbstractPresence$Type/PROBE
   :subscribe rocks.xmpp.core.stanza.model.AbstractPresence$Type/SUBSCRIBE
   :subscribed rocks.xmpp.core.stanza.model.AbstractPresence$Type/SUBSCRIBED
   :unavailable rocks.xmpp.core.stanza.model.AbstractPresence$Type/UNAVAILABLE
   :unsubscribe rocks.xmpp.core.stanza.model.AbstractPresence$Type/UNSUBSCRIBE
   :unsubscribed rocks.xmpp.core.stanza.model.AbstractPresence$Type/UNSUBSCRIBED})

(def ^{:doc "Session status translation map."
       :dynamic true}
  *presence-show-types*
  {:away rocks.xmpp.core.stanza.model.AbstractPresence$Show/AWAY
   :chat rocks.xmpp.core.stanza.model.AbstractPresence$Show/CHAT
   :dnd rocks.xmpp.core.stanza.model.AbstractPresence$Show/DND
   :xa rocks.xmpp.core.stanza.model.AbstractPresence$Show/XA})


(defn translate-session-status
  [status]
  (get (map-invert *session-statuses*) status :unknown))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Stanza transformation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol IStanza
  (get-from [_])
  (get-to [_]))

(defprotocol IStanzaType
  (get-type [_]))

(defprotocol IStanzaBody
  (get-body [_]))


(extend-type Message
  IStanza
  (get-to [this] (.getTo this))
  (get-from [this] (.getFrom this))

  IStanzaType
  (get-type [this]
    (get (map-invert *message-types*) (.getType this)))

  IStanzaBody
  (get-body [this] (.getBody this)))


(extend-type Presence
  IStanza
  (get-to [this] (.getTo this))
  (get-from [this] (.getFrom this))

  IStanzaType
  (get-type [this]
    (get (map-invert *presence-types*) (.getType this)))

  IStanzaBody
  (get-body [this] (.getBody this)))

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
    ;; (.defaultResponseTimeout builder 50000)
    (.build builder)))

(defn make-session
  "Create new not connected session."
  [{:keys [domain] :as cfg}]
  (let [connection-cfg (make-connection-config cfg)
        session-cfg (make-session-config cfg)
        connection-cfgs (into-array TcpConnectionConfiguration [connection-cfg])
        session (XmppSession. domain session-cfg connection-cfgs)]

    ;; Explicitly enable ping
    ;; (let [pingmanager (.getExtensionManager session (.-class PingManager))]
    ;;   (.setEnabled pingmanager true))

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


(defn wait-close
  [session]
  (let [channel (chan 1)
        listener (reify SessionStatusListener
                    (sessionStatusChanged [this event]
                      (let [status (translate-session-status (.getStatus event))]
                        (when (or (= status :closing)
                                  (= status :closed))
                          (close! channel)
                          (.removeSessionStatusListener session this)))))]
     (.addSessionStatusListener session listener)
     channel))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Session listeners
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn listen-session-status
  "Lister session statuses and return a channel."
  ([session] (listen-session-status session (chan)))
  ([session channel]
   (let [listener (reify SessionStatusListener
                    (sessionStatusChanged [this event]
                      (let [status (translate-session-status (.getStatus event))
                            rsp (put! channel status)]
                        (when-not (true? rsp)
                          (.removeSessionStatusListener session this)))))]
     (.addSessionStatusListener session listener)

     ;; Wait session close events and
     ;; close resources when connection is closed.
     (go
       (<! (wait-close session))
       (close! channel))

     channel)))

(defn listen-messages
  "Listen messages from the session."
  ([session] (listen-messages session (chan)))
  ([session channel]
   (let [listener (reify MessageListener
                    (handle [this event]
                      (let [message (.getMessage event)
                            rsp (put! channel message)]
                        (when-not (true? rsp)
                          (.removeMessageListener session this)))))]

     (.addMessageListener session listener)

     ;; Wait session close events and
     ;; close resources when connection is closed.
     (go
       (<! (wait-close session))
       (close! channel))

     channel)))

(defn listen-presence
  "Listen presence from the session."
  ([session] (listen-presence session (chan)))
  ([session channel]
   (let [listener (reify PresenceListener
                    (handle [this event]
                      (let [presence (.getPresence event)
                            rsp (put! channel presence)]
                        (when-not (true? rsp)
                          (.removePresenceListener session this)))))]
     (.addPresenceListener session listener)

     ;; Wait session close events and
     ;; close resources when connection is closed.
     (go
       (<! (wait-close session))
       (close! channel))

     channel)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Subscriptions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn approve-contact-subscription
  [session from]
  (let [manager (.getPresenceManager session)]
    (.approveSubscription manager from)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; MUC
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn make-jid
  ([domain] (Jid. domain))
  ([local domain] (Jid. local domain))
  ([local domain resource] (Jid. local domain resource)))

(defn get-muc-manager
  [session]
  (.getExtensionManager session MultiUserChatManager))

(defn get-chat-service
  [session roomdomain]
  (let [manager (get-muc-manager session)]
    (.createChatService manager (Jid. roomdomain))))

(defn get-room
  [chatservice roomname]
  (.createRoom chatservice roomname))

(defn listen-muc-invitations
  "Listen presence from the session."
  ([session mucmanager] (listen-muc-invitations session mucmanager (chan)))
  ([session mucmanager channel]
   (let [listener (reify InvitationListener
                    (invitationReceived [this invitation]
                      (let [inviter (.getInviter invitation)
                            room    (.getRoomAddress invitation)
                            rsp (put! channel invitation)]
                        (when-not (true? rsp)
                          (.removeInvitationListener mucmanager this)))))]

     (.addInvitationListener mucmanager listener)

     ;; Wait session close events and
     ;; close resources when connection is closed.
     (go
       (<! (wait-close session))
       (close! channel))

     channel)))

