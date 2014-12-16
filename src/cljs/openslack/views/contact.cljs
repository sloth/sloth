(ns openslack.views.contact
  (:require [om.core :as om :include-macros true]
            [sablono.core :as s :include-macros true]
            [openslack.state :as st]
            [openslack.views.message :as msg]
            [openslack.xmpp :as xmpp]))

(defn send-content-message
  [user message]
  (let [bare-jid (get-in user [:jid :bare])
        client (:client @st/state)
        msg {:to bare-jid
             :type :chat
             :body message}]
    (xmpp/send-message client msg)))

(defn contact
  [state owner]
  (reify
    om/IDisplayName
    (display-name [_] "Contact")

    om/IInitState
    (init-state [_] {:message ""})

    om/IRenderState
    (render-state [_ {:keys [message]}]
      (when-let [contact-name (get-in state [:page :contact])]
        (let [user (st/contact contact-name)
              presence (st/get-presence (:jid user))]
          (s/html
           [:section.client-main
            [:header
             [:h1 (str "@" contact-name)]
             [:h2
              (:status presence)
              ]]
            [:hr]
            [:div.chat-zone
             [:div.chat-container
              [:div.messages-container
               (om/build-all msg/message (st/contact-messages user))]
              [:div.write-message
               [:textarea {:on-key-up (fn [e]
                                        (let [value (-> e
                                                        (.-target)
                                                        (.-value))]
                                          (om/set-state! owner :message value)))}]
               [:button {:on-click (fn [e]
                                     (.preventDefault e)
                                     (send-content-message user message)
                                     (om/set-state! owner :message "")
                                     )} "Send"]]]
             [:div.chat-sidebar-holder [:div]]]]))))))
