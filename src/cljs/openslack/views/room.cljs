(ns openslack.views.room
  (:require [om.core :as om :include-macros true]
            [sablono.core :as s :include-macros true]
            [openslack.state :as st]))

(defn message
  [state owner]
  (reify
    om/IDisplayName
    (display-name [_] "Message")

    om/IRender
    (render [_]
      (s/html
           [:div.message
            [:div.message-avatar
             [:img
              {:height "35",
               :width "35",
               :alt "#user",
               :src (:avatar state)}]]
            [:div.message-content
             [:div.message-title [:strong (-> state :from :local)] [:span "10:11 pm"]]
             [:p.content (-> state :body)]]]))))

(defn room
  [state owner]
  (reify
    om/IDisplayName
    (display-name [_] "Room")

    om/IRender
    (render [_]
      (when-let [room (-> state :page :room)]
        (s/html
         [:section.client-main
          [:header
           [:h1 (str "#" room)]
           [:h2
            "Le topic del dia: Los "
            [:strong "Sloth"]
            " dominaran el mundo\n        "]]
          [:hr]
          [:div.chat-zone
           [:div.chat-container
            [:div.messages-container
             (om/build-all message (st/room-messages room))]
            [:div.write-message [:textarea " "] [:button "Send"]]]
           [:div.chat-sidebar-holder [:div]]]])))))
