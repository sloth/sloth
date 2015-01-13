(ns sloth.chat
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [<!]]
            [shodan.console :as console :include-macros true]
            [sloth.xmpp :as xmpp]
            [sloth.types :as types]
            [sloth.state :as st]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Messaging
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn send-group-message
  [state room message]
  (let [client (:client state)
        recipient (:bare room)]
    (when client
      (xmpp/send-message client {:to recipient
                                 :type :groupchat
                                 :body message}))))

(defn send-personal-message
  "Given a state, recipient and body of message
  it constructs a chat instance and sends it
  thought the xmpp connection.

  Additionally adds it to the local state because
  p2p messages does not returns like in muc."
  [state to message]
  (when-let [client (st/get-client)]
    (let [user (st/get-logged-user)
          msg (types/->chat {:to to
                             :from user
                             :type :chat
                             :timestamp (js/Date.)
                             :body message})]
      (xmpp/send-message client msg)
      (st/insert-private-message to msg))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Presence
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn set-status
  [state status]
  (let [client (st/get-client)
        user-presence (st/get-presence (:user state))]
      (xmpp/send-presence client (assoc user-presence :status status
                                                      :priority 42))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Bookmarks
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn bookmark-room
  [room]
  (when-let [client (st/get-client)]
    (let [bookmark {:autojoin true
                    :name (get-in room [:jid :local])
                    :jid (:jid room)}]
    (.addBookmark client (clj->js bookmark)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; MUC
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn set-room-subject
  [roomname subject]
  (when-let [client (st/get-client)]
    (.setSubject client roomname subject)))

(defn accept-room-invitation
  [room]
  (when-let [client (st/get-client)]
    (xmpp/accept-subscription client (:bare room))
    (go
      (let [r (<! (xmpp/join-room client
                                  (:bare room)
                                  (types/get-user-local (st/get-logged-user))))]
        (st/insert-room r)
        (bookmark-room r)))))

(defn decline-room-invitation
  [room]
  (when-let [client (st/get-client)]
    (xmpp/deny-subscription client (:bare room))))
