(ns openslack.views.roster
  (:require [om.core :as om :include-macros true]
            [sablono.core :as s :include-macros true]))

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
                  (for [contact (:roster state)]
                    [:li.unread
                     [:span.status.online]
                     (get-in contact [:jid :local])
                     [:i "3"]
                     [:div.read-status "Lorem ipsum dolor sit amet."]])]])))))
