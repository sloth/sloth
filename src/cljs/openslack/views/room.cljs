(ns openslack.views.room
  (:require [om.core :as om :include-macros true]
            [sablono.core :as s :include-macros true]
            [openslack.state :as st]
            [openslack.views.messages :as msg]
            [openslack.communicator :as comm]))

(defn room
  [state owner]
  (reify
    om/IDisplayName
    (display-name [_] "Room")

    om/IInitState
    (init-state [_] {:message ""})

    om/IRenderState
    (render-state [_ {:keys [message]}]
      (when-let [room-name (get-in state [:page :room])]
        (let [r (st/room room-name)]
          (s/html
           [:section.client-main
            [:header
             [:h1 (str "#" (get-in r [:jid :local]))]
             [:h2
              "Le topic del dia: Los "
              [:strong "Sloth"]
              " dominaran el mundo\n        "]]
            [:hr]
            [:div.chat-zone
             [:div.chat-container
              [:div.messages-container
               (om/build-all msg/room-message (st/room-messages r))]
              [:div.write-message
               [:textarea {:on-key-up (fn [e]
                                        (let [value (-> e
                                                        (.-target)
                                                        (.-value))]
                                          (om/set-state! owner :message value)))}]
               [:button {:on-click (fn [e]
                                     (.preventDefault e)
                                     (when message
                                       (comm/send-group-message (get-in r [:jid :bare]) message)
                                       (om/set-state! owner :message ""))
                                     )} "Send"]]]
             [:div.chat-sidebar-holder [:div]]]]))))))
