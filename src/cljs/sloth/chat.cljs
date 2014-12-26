(ns sloth.chat
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [<!]]
            [shodan.console :as console :include-macros true]
            [sloth.xmpp :as xmpp]
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
  [state user message]
  (let [loggeduser (st/get-logged-user state)
        client (st/get-client state)
        source (:bare loggeduser)
        recipient (:bare user)
        msg-id (str (gensym source))]
    (when client
      (xmpp/send-message client {:to recipient
                                 :type :chat
                                 :body message
                                 :id msg-id})
      ;; (console/log "send-personal-message"
      ;;              (pr-str user)
      ;;              (pr-str loggeduser))

      (st/insert-private-message user {:to user
                                       :from loggeduser
                                       :type :chat
                                       :timestamp (js/Date.)
                                       :body message
                                       :id msg-id}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Presence
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn set-status
  [state status]
  (let [client (st/get-client state)
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
                                  (:local (:user @st/state))))]
        (st/add-room r)
        (bookmark-room r)))))

(defn decline-room-invitation
  [room]
  (when-let [client (st/get-client)]
    (xmpp/deny-subscription client (:bare room))))
