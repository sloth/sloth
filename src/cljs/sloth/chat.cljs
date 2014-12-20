(ns sloth.chat
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [<!]]
            [shodan.console :as console :include-macros true]
            [sloth.xmpp :as xmpp]
            [sloth.state :as st]))

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
        recipient (:bare user)]
    (when client
      (xmpp/send-message client {:to recipient
                                 :type :chat
                                 :body message})
      ;; (console/log "send-personal-message"
      ;;              (pr-str user)
      ;;              (pr-str loggeduser))

      (st/insert-private-message user {:to user
                                       :from loggeduser
                                       :type :chat
                                       :timestamp (js/Date.)
                                       :body message}))))

(defn set-status
  [state status]
  (when-let [client (st/get-client state)]
    (let [user-presence (st/get-presence (:user state))]
      (xmpp/send-presence client (assoc user-presence :status status)))))
