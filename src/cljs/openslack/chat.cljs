(ns openslack.chat
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [<!]]
            [shodan.console :as console :include-macros true]
            [openslack.xmpp :as xmpp]
            [openslack.state :as st]))

(defn send-group-message
  [recipient body]
  (when-let [client (:client @st/state)]
    (xmpp/send-message client {:to recipient
                               :type :groupchat
                               :body body})))

(defn send-personal-message
  [recipient body]
  (when-let [client (:client @st/state)]
    (console/log 111)
    (let [message {:to recipient
                   :type :chat
                   :from (st/logged-in-user @st/state)
                   :body body}]

      ;; TODO: handle duplicate msg errors
      (xmpp/send-message client message)
      (swap! st/state st/add-own-chat
             (assoc message :timestamp (js/Date.))))))
