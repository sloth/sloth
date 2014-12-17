(ns openslack.views.contact
  (:require [om.core :as om :include-macros true]
            [sablono.core :as s :include-macros true]
            [openslack.state :as st]
            [openslack.views.messages :as msg]
            [openslack.chat :as chat]))

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
        (let [user (st/contact @st/state contact-name)
              presence (st/get-presence @st/state (:jid user))
              send-msg-fn (partial chat/send-personal-message (get-in user [:jid :bare]))]
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
               (om/build-all msg/contact-message (st/contact-messages @st/state user))]
              (om/build (msg/message-input send-msg-fn) state)]
             [:div.chat-sidebar-holder [:div]]]]))))))
