(ns openslack.chat
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [<!]]
            [shodan.console :as console :include-macros true]
            [openslack.xmpp :as xmpp]
            [openslack.state :as st]))

(defn send-group-message
  [state room message]
  (let [client (:client state)
        recipient (get-in room [:jid :bare])]
    (when client
      (xmpp/send-message client {:to recipient
                                 :type :groupchat
                                 :body message}))))

(defn send-personal-message
  [state user message]
  (let [loggeduser (st/get-logged-user state)
        client (st/get-client state)
        source (get-in loggeduser [:jid :bare])
        recipient (get-in user [:jid :bare])]

    (when client
      (xmpp/send-message client {:to recipient
                                 :type :chat
                                 :body message})

      (st/insert-message user {:to user
                               :from loggeduser
                               :type :chat
                               :timestamp (js/Date.)
                               :body message}))))

