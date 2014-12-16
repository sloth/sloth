(ns openslack.views.room
  (:require [om.core :as om :include-macros true]
            [sablono.core :as s :include-macros true]
            [openslack.state :as st]
            [openslack.views.message :refer [message]]))

(defn room
  [state owner]
  (reify
    om/IDisplayName
    (display-name [_] "Room")

    om/IRender
    (render [_]
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
               (om/build-all message (st/room-messages r))]
              [:div.write-message [:textarea " "] [:button "Send"]]]
             [:div.chat-sidebar-holder [:div]]]]))))))
