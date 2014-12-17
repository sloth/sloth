(ns openslack.views.room
  (:require [om.core :as om :include-macros true]
            [sablono.core :as s :include-macros true]
            [openslack.state :as st]
            [openslack.views.messages :as msg]
            [openslack.chat :as chat]))

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
        (let [r (st/room room-name)
              bare-jid (get-in r [:jid :bare])
              send-msg-fn (partial chat/send-group-message bare-jid)]
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
             (om/build (msg/message-input send-msg-fn) state)]
             [:div.chat-sidebar-holder [:div]]]]))))))
