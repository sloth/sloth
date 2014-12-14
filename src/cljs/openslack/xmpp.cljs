(ns openslack.xmpp
  (:require [cljs.core.async :as async]
            [cats.monad.either :as either]))

;; Client

(defn create-client [config]
  (.createClient js/XMPP (clj->js config)))

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
  (let [c (async/chan 1)]
    (connect client)
    (.once client "session:started" (fn [rjid]
                                      (->> (if (= (.-type rjid) "error")
                                             (either/left (keyword (.-condition (.-error rjid))))
                                             (either/right (raw-jid->jid rjid)))
                                           (async/put! c))))
    c))

;; Roster

(defn raw-roster->roster [rroster]
  (into [] (map (fn [ritem]
                  {:jid (raw-jid->jid (.-jid ritem))
                   :subscription (keyword (.-subscription ritem))})
                (.-items (.-roster rroster)))))

(defn get-roster [client]
  (let [c (async/chan 1)]
    (.getRoster client (fn [_ rroster]
                          (->> (if (= (.-type rroster) "error")
                                 (either/left (keyword (.-condition (.-error rroster))))
                                 (either/right (raw-roster->roster rroster)))
                                (async/put! c))))
    c))

(defn raw-roster-update->roster-update [rrupdate]
  {:jid (raw-jid->jid (.-jid rrupdate))
   :subscription (keyword (.-subscription rrupdate))})

(defn roster-updates [client]
  (let [c (async/chan)]
    (.on client "roster:update" (fn [rroster]
                                  (async/onto-chan c
                                                   (map raw-roster-update->roster-update
                                                        (.-items (.-roster rroster)))
                                                   false)))
    c))

(defn update-roster-item [client item]
  (let [c (async/chan 1)]
    (.updateRosterItem client (clj->js item) (fn [_ ritem]
                                                (->> (if (= (.-type ritem) "error")
                                                       (either/left (keyword (.-condition (.-error ritem))))
                                                       (either/right (js->clj ritem)))
                                                     (async/put! c))))
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

(defn raw-chat->chat [rchat]
  {:body (.-body rchat)
   :type (keyword (.-type rchat))
   :from (raw-jid->jid (.-from rchat))
   :to (raw-jid->jid (.-to rchat))})

(defn chats [client]
  (let [c (async/chan 10 (map raw-chat->chat))]
    (.on client "chat" (partial async/put! c))
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
  {:from (raw-jid->jid (.-from rroom))
   :type (keyword (.-type rroom))})

(defn join-room [client room nick]
  (let [c (async/chan 10 (map (comp either/right raw-room->room)))]
    (.once client "muc:join" (partial async/put! c))
    (.joinRoom client room nick)
    c))
