(ns openslack.chat
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [<!]]
            [openslack.xmpp :as xmpp]
            [openslack.state :as st]))

;; (defn join-room
;;   [client roomaddress alias nick]
;;   (go
;;     (let [room (<! (xmpp/join-room roomaddress nickname))]

(defn send-group-message
  [recipient body]
  (when-let [client (:client @st/state)]
    (xmpp/send-message client {:to recipient
                               :type :groupchat
                               :body body})))

(defn send-personal-message
  [recipient body]
  (when-let [client (:client @st/state)]
    (let [message {:to recipient
                   :type :chat
                   :from (st/logged-in-user @st/state)
                   :body body}]
    ; TODO: handle duplicate msg errors
    (xmpp/send-message client message)
    (swap! st/state st/add-own-chat (assoc message :timestamp (js/Date.))))))
