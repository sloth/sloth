(ns openslack.views.room
  (:require [om.core :as om :include-macros true]
            [sablono.core :as s :include-macros true]
            [openslack.state :as st]
            [openslack.views.message :as msg]
            [openslack.xmpp :as xmpp]))

(defn send-content-message
  [room message]
  (let [bare-room (get-in room [:jid :bare])
        client (:client @st/state)
        msg {:to bare-room
             :type :groupchat
             :body message}]
    (xmpp/send-message client msg)))

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
          (.log js/console "RRRR " (pr-str room-name) (pr-str r))
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
               (om/build-all msg/message (st/room-messages r))]
              [:div.write-message
               [:textarea {:on-key-up (fn [e]
                                        (let [value (-> e
                                                        (.-target)
                                                        (.-value))]
                                          (om/set-state! owner :message value)))}]
               [:button {:on-click (fn [e]
                                     (.preventDefault e)
                                     (send-content-message r message)
                                     (om/set-state! owner :message "")
                                     ; clean textarea ()
                                     )} "Send"]]]
             [:div.chat-sidebar-holder [:div]]]]))))))
