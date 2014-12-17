(ns openslack.views.roster
  (:require [om.core :as om :include-macros true]
            [sablono.core :as s :include-macros true]
            [openslack.routing :refer [navigate contact-route]]
            [openslack.state :as st]))

; TODO: unread chats
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
                        :let [presence (st/get-presence @st/state contact)
                              name (get-in contact [:jid :local])
                              is-current-contact? (and (= (get-in state [:page :state]) :contact)
                                                       (= (get-in state [:page :contact]) name))
                              attrs {:on-click #(navigate (contact-route {:name name}))
                                     :class-name (when is-current-contact? "highlighted")}]]
                    [:li attrs
                     (condp = (:availability presence)
                       :available [:span.status.online]
                       :unavailable [:span.status.offline]
                       :dnd [:span.status.busy]
                       [:span.status.offline])
                      name
;                     [:i "3"]
                     [:div.read-status (:status presence)]])
                  [:li.add [:span "+"] "Add new contact\n              "]]])))))
