(ns sloth.xmpp
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :as async :refer [<! timeout put! chan close!]]
            [shodan.console :as console :include-macros true]
            [sloth.config :as config]
            [sloth.types :as types]
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

(defn send-message
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
 [client {:keys [to id type body]}]
 (let [message #js {:to (types/get-user-bare to)
                    :id id
                    :type (case type
                            :sloth.types/chat "chat"
                            :sloth.types/groupchat "groupchat")
                    :body body}]
   (console/log "xmpp/send-message" message)
   (.sendMessage client message)))

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
                    (put! c))))
    c))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Sesssion
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn start-session [client]
  (let [c (async/chan 1)
        cleanup (fn []
                  (async/close! c)
                  (.off client "session:*"))]
    (connect client)
    (.on client "session:*" (fn [ev rjid]
                              (console/log "Kaka")
                              (condp = ev
                                "session:started"
                                (do
                                  (put! c (either/right (types/rjid->jid rjid)))
                                  (cleanup))

                                "session:error"
                                (do
                                  (put! c (either/left))
                                  (cleanup))

                                nil)))
    c))

(defn authenticate
  [username password]
  (go
    (console/log "xmpp/authenticate" username password)
    (let [config (<! (config/get-xmpp-config))
          client (-> config
                     (assoc :jid username :password password)
                     (create-client))
          muser  (<! (start-session client))]
      (console/log (pr-str muser))
      (m/>>= muser
             (fn [user]
               (m/return {:user user
                          :client client
                          :auth {:username username
                                 :password password}}))))))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Roster
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- transform-roster-item
  "Given raw roster item transform it to
  jid instance."
  [roster-entry]
  (let [jid (types/rjid->jid (.-jid roster-entry))
        sub (keyword (.-subscription roster-entry))
        groups (when-let [groups (.-groups roster-entry)]
                 (into [] groups))]
    (types/->roster-item {:jid jid
                          :subscription sub
                          :groups groups})))

(defn- transform-roster
  "Given a raw roster instance, transform it to vector
  of jid instances."
  [rroster]
  (let [rroster (.-roster rroster)
        items (.-items rroster)]
    (map transform-roster-item items)))

(defn- on-roster-received
  [channel rroster]
  (let [type (keyword (.-type rroster))
        rsp  (cond
               (= type :error)
               (let [error (.-error rroster)
                     condition (.-condition error)]
                 (either/left condition))

               :else
               (let [roster (transform-roster rroster)]
                 (either/right roster)))]
    (put! channel rsp)
    (close! channel)
    rsp))

(defn get-roster
  "Given a client, return a channel that will be resolved
  with roster. Roster is represented with a seq of
  jid instances."
  ([client] (get-roster client (chan 1)))
  ([client channel]
   (.getRoster client #(on-roster-received channel %2))
   channel))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Subscriptions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
                          (put! c rblocked)))
    c))

; FIXME: returns a server error
(defn block [client jid]
  (let [c (async/chan 1)]
    (.block client jid (fn [rblock]
                         (put! c rblock)))
    c))

; FIXME: returns a server error
(defn unblock [client jid]
  (let [c (async/chan 1)]
    (.unblock client jid (fn [rblock]
                           (put! c rblock)))
    c))

(defn subscriptions [client]
  (let [c (async/chan)]
    (.on client "subscribed" (partial put! c))
    c))

(defn subscription-requests [client]
  (let [c (async/chan)]
    (.on client "subscribe" (fn [rsubscription]
                              (put! c {:subscription rsubscription})))
    c))

(defn unsubscriptions [client]
  (let [c (async/chan)]
    (.on client "unsubscribed" (partial put! c))
    c))

(defn unsubscription-requests [client]
  (let [c (async/chan)]
    (.on client "unsubscribe" (partial put! c))
    c))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Presences
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- transform-presence
  "Transform raw xmpp presence object to
  sloth Presence instance."
  [rpresence]
  (types/->presence {:from (types/rjid->jid (.-from rpresence))
                     :to (types/rjid->jid (.-to rpresence))
                     :status (.-status rpresence)
                     :availability (keyword (.-type rpresence))
                     :priority (.-priority rpresence)}))

(defn presences
  "Get presences information."
  ([client] (presences client (chan 10 (map transform-presence))))
  ([client channel]
   (.on client "presence" (partial put! channel))
   channel))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Service discovery
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-features [client]
  (js->clj (.-features (.-discoInfo (.getCurrentCaps client)))))

(defn update-capabilities [client]
  (.updateCaps client))

; TODO: Error handling
(defn disco-info [client jid node]
  (let [c (async/chan 1)]
    (.getDiscoInfo client jid node (fn [_ rinfo]
                                     (put! c rinfo)))
    c))

; TODO: Error handling
(defn disco-items [client jid node]
  (let [c (async/chan 1)]
    (.getDiscoItems client jid node (fn [_ ritems]
                                      (put! c ritems)))
    c))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Chats
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn is-message?
  [rchat]
  ;; TODO: Filter out subject messages too
  (undefined? (.-mucInvite rchat)))

(defn chats
  ([client]
   (let [channel (chan 1 (comp (filter is-message?)
                               (map types/rchat->chat)))]
     (chats client channel)))
  ([client channel]
   (.on client "chat" (partial put! channel))
   (.on client "groupchat" (partial put! channel))
   channel))

(defn raw-chat-state->chat-state [rchatstate]
  {:from (types/rjid->jid (.-from rchatstate))
   :state (keyword (.-chatState rchatstate))})

(defn chat-states [client]
  (let [c (async/chan 10 (map raw-chat-state->chat-state))]
    (.on client "chat:state" (partial put! c))
    c))

;; MUC

(defn raw-room->room
  [rroom]
  ; TODO: more complete info: topic & co
  {:jid (types/rjid->jid (.-from rroom))
   :muc (js->clj (.-muc rroom) {:keywordize-keys true}) ; FIXME
   :type (keyword (.-type rroom))})

(defn join-room
  ([client room nick]
   (join-room client room nick {:history {:maxstanzas 50}}))
  ([client room nick {:keys [history] :or {:history {:maxstanzas 50}}}]
   (let [c (async/chan 10 (map raw-room->room))]
     (.once client "muc:join" (partial put! c))
     (.joinRoom client room nick (clj->js history))
     c)))

(defn raw-subject->subject
  [rsubject]
  {:room (types/rjid->jid (.-from rsubject))
   :subject (.-subject rsubject)})

(defn subjects [client]
  (let [c (async/chan 10 (map raw-subject->subject))]
    (.on client "muc:subject" (partial put! c))
    c))

(defn raw-room-invitation->room-invitation
  [rinv]
  {:from (types/rjid->jid (.-from rinv))
   :room (types/rjid->jid (.-room rinv))
   :type (keyword (.-type rinv))})

(defn room-invitations [client]
  (let [c (async/chan 10 (map raw-room-invitation->room-invitation))]
    (.on client "muc:invite" (partial put! c))
    c))

;; Bookmarks

(defn raw-bookmarks->bookmarks
  [rbookmarks]
  (let [storage (.-privateStorage rbookmarks)
        bookmarks (.-bookmarks storage)]
    {:conferences (map #(types/rjid->jid (.-jid %)) (.-conferences bookmarks))}))

(defn get-bookmarks
  [client]
  (let [c (async/chan 1 (map raw-bookmarks->bookmarks))]
    (.getBookmarks client (fn [_ rbookmarks]
                            (put! c rbookmarks)
                            (close! c)))
    c))
