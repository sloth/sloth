(ns openslack.views.roster
  (:require [om.core :as om :include-macros true]
            [sablono.core :as s :include-macros true]
            [openslack.state :as st]))

; TODO: unread chats from user
(defn roster
  [state owner]
  (reify
    om/IDisplayName
    (display-name [_] "Roster")

    om/IRender
    (render [_]
      (when (:roster state)
        (s/html [:div.room-list.sidebar-list
                 [:h3 "Contact List"]
                 [:ul
                  (for [contact (:roster state)
                        :let [presence (st/get-presence contact)]]
                    [:li ;.unread
                     (condp = (:availability presence)
                       :available [:span.status.online]
                       :dnd [:span.status.busy]
                       :offline [:span.status.offline]
                       [:span.status.offline])
                     (get-in contact [:jid :local])
;                     [:i "3"]
                     [:div.read-status (:status presence)]])]])))))
