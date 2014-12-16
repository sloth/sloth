(ns openslack.communicator
  (:require [openslack.xmpp :as xmpp]
            [openslack.state :as st]
            [om.core :as om]))

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
                   :from (get-in @st/state [:user :full])
                   :body body}]
    ; TODO: handle duplicate msg errors
    (xmpp/send-message client message)
    (swap! st/state st/add-own-chat (assoc message :timestamp (js/Date.))))))
