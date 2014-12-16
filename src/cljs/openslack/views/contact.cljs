(ns openslack.views.contact
  (:require [om.core :as om :include-macros true]
            [sablono.core :as s :include-macros true]
            [openslack.state :as st]
            [openslack.views.messages :as msg]
            [openslack.communicator :as comm]))

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
               (om/build-all msg/contact-message (st/contact-messages user))]
              [:div.write-message
               [:textarea {:on-key-up (fn [e]
                                        (let [value (-> e
                                                        (.-target)
                                                        (.-value))]
                                          (om/set-state! owner :message value)))}]
               [:button {:on-click (fn [e]
                                     (.preventDefault e)
                                     (when message
                                       (comm/send-personal-message (get-in user [:jid :bare]) message)
                                       (om/set-state! owner :message ""))
                                     )} "Send"]]]
             [:div.chat-sidebar-holder [:div]]]]))))))
