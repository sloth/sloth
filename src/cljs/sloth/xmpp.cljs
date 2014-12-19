(ns sloth.xmpp
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :as async :refer [<! timeout put! chan close!]]
            [shodan.console :as console :include-macros true]
            [sloth.config :as config]
            [cats.core :as m :include-macros true]
            [cats.monad.either :as either]))

;; Client

(defn create-client [{:keys [jid password transport url]}]
  "Given a dictionary with XMPP credentials, return a client instance.

  Examples:

    ; Client with BOSH transport
    (create-client {:jid \"dialelo@niwi.be\"
                    :password \"dragon\"
                    :transport :bosh
                    :url \"http://niwi.be:5280/http-bind\"})
  "
  (let [config {:jid jid :password password :transports [transport]}
        url-param (.slice (str transport "URL") 1)
        config (assoc config url-param url)]
    (.createClient js/XMPP (clj->js config))))

(defn connect [client]
  (.connect client))

(defn disconnect [client]
  (.disconnect client))

(defn send-presence
  "Send a presence stanza.

     http://xmpp.org/rfcs/rfc3921.html#presence

  Examples:

   ; Simple presence signaling
   (xmpp/send-presence client)

   ; Presence with status information
   (xmpp/send-presence client {:status \"Happy!\"})

   ; Presence with availability and status information
   (xmpp/send-presence client {:status \"Busy!\"
                               :show :dnd})

   (xmpp/send-presence client {:status \"Away!\"
                               :show :away})
  "
  ([client]
     (.sendPresence client))
  ([client presence]
     (.sendPresence client (clj->js presence))))

(defn send-message [client msg]
  "Send a message stanza.

     http://xmpp.org/rfcs/rfc3921.html#messaging

  Examples:

   ; A direct message to a user
   (xmpp/send-presence client {:to \"niwi@niwi.be\"
                               :type :chat
                               :body \"How are you?\"})

   ; A message to a group chat
   (xmpp/send-presence client {:to \"room@conference.niwi.be\"
                               :type :groupchat
                               :body \"How are y'all?\"})
  "
  (.sendMessage client (clj->js msg)))

(defn send-iq [client iq]
  "Send an IQ stanza. Returns a channel that will eventually have
   the value of the result."
  (let [c (async/chan 1)]
    (.sendIq client
             (clj->js iq)
             (fn [riq]
               (->> (if (= (.-type riq) "error")
                      (either/left (keyword (.-condition (.-error riq))))
                      (either/right riq))
                    (async/put! c))))
    c))

;; Session

(defn raw-jid->jid
  [rjid]
  {:bare (.-bare rjid)
   :local (.-local rjid)
   :domain (.-domain rjid)
   :resource (.-resource rjid)
   :full (.-full rjid)})

(defn start-session [client]
  (let [c (async/chan 1)
        cleanup (fn []
                  (async/close! c)
                  (.off client "session:*"))]
    (connect client)

    (.on client "session:*" (fn [ev rjid]
                              (condp = ev
                                "session:started"
                                (do
                                  (async/put! c (either/right (raw-jid->jid rjid)))
                                  (cleanup))
                                "session:error"
                                (do
                                  (async/put! c (either/left))
                                  (cleanup))
                                nil)))
    c))

(defn authenticate
  [username password]
  (go
    (let [config (<! (config/get-xmpp-config))
          client (-> config
                     (assoc :jid username :password password)
                     (create-client))
          muser  (<! (start-session client))]
      (m/>>= muser
             (fn [user]
               (m/return {:user user
                          :client client
                          :auth {:username username
                                 :password password}}))))))
;; Roster

(defn raw-roster->roster
  [rroster]
  (let [transformfn (fn [rosteritem]
                      (let [item (raw-jid->jid (.-jid rosteritem))]
                        [(keyword (:local item))
                         (assoc item :subscription (keyword (.-subscription rosteritem)))]))]
    (into {} (map transformfn (.-items (.-roster rroster))))))

(defn get-roster
  [client]
  (let [c (async/chan 1)]
    (.getRoster client (fn [_ rroster]
                          (->> (if (= (.-type rroster) "error")
                                 (either/left (keyword (.-condition (.-error rroster))))
                                 (either/right (raw-roster->roster rroster)))
                                (put! c))
                          (close! c)))
    c))

(defn accept-subscription [client jid]
  (.acceptSubscription client jid))

(defn deny-subscription [client jid]
  (.denySubscription client jid))

(defn subscribe [client jid]
  (.subscribe client jid))

(defn unsubscribe [client jid]
  (.unsubscribe client jid))

(defn get-blocked [client]
  (let [c (async/chan 1)]
    (.getBlocked client (fn [_ rblocked]
                          (async/put! c rblocked)))
    c))

; FIXME: returns a server error
(defn block [client jid]
  (let [c (async/chan 1)]
    (.block client jid (fn [rblock]
                         (async/put! c rblock)))
    c))

; FIXME: returns a server error
(defn unblock [client jid]
  (let [c (async/chan 1)]
    (.unblock client jid (fn [rblock]
                           (async/put! c rblock)))
    c))

(defn subscriptions [client]
  (let [c (async/chan)]
    (.on client "subscribed" (partial async/put! c))
    c))

(defn unsubscriptions [client]
  (let [c (async/chan)]
    (.on client "unsubscribed" (partial async/put! c))
    c))

(defn raw-presence->presence
  [rpr]
  {:from (raw-jid->jid (.-from rpr))
   :status (.-status rpr)
   :type (keyword (.-type rpr))
   :to (raw-jid->jid (.-to rpr))})

(defn presences
  [client]
  (let [c (async/chan 10 (map raw-presence->presence))]
    (.on client "presence" (partial async/put! c))
    c))

;; Service discovery

(defn get-features [client]
  (js->clj (.-features (.-discoInfo (.getCurrentCaps client)))))

(defn update-capabilities [client]
  (.updateCaps client))

; TODO: Error handling
(defn disco-info [client jid node]
  (let [c (async/chan 1)]
    (.getDiscoInfo client jid node (fn [_ rinfo]
                                     (async/put! c rinfo)))
    c))

; TODO: Error handling
(defn disco-items [client jid node]
  (let [c (async/chan 1)]
    (.getDiscoItems client jid node (fn [_ ritems]
                                      (async/put! c ritems)))
    c))

;; Chats

(defn raw-chat->chat
  [rchat]
  (let [chat (transient {:id (.-id rchat)
                         :body (.-body rchat)
                         :type (keyword (.-type rchat))
                         :from (raw-jid->jid (.-from rchat))
                         :to (raw-jid->jid (.-to rchat))
                         :timestamp (js/Date.)})]
    (when-let [delay (.-delay rchat)]
      (assoc! chat
              :timestamp (.-stamp delay)
              :delay (js->clj delay)))
    (persistent! chat)))

(defn chats [client]
  (let [c (async/chan 10 (map raw-chat->chat))]
    (.on client "chat" (fn [e] (put! c e)))
    (.on client "groupchat" (fn [e] (put! c e)))
    c))

(defn raw-chat-state->chat-state [rchatstate]
  {:from (raw-jid->jid (.-from rchatstate))
   :state (keyword (.-chatState rchatstate))})

(defn chat-states [client]
  (let [c (async/chan 10 (map raw-chat-state->chat-state))]
    (.on client "chat:state" (partial async/put! c))
    c))

;; MUC

(defn raw-room->room
  [rroom]
  ; TODO: more complete info: topic & co
  {:jid (raw-jid->jid (.-from rroom))
   :muc (js->clj (.-muc rroom) {:keywordize-keys true}) ; FIXME
   :type (keyword (.-type rroom))})

(defn join-room [client room nick]
  (let [c (async/chan 10 (map raw-room->room))]
    (.once client "muc:join" (partial async/put! c))
    (.joinRoom client room nick)
    c))