(ns openslack.views.roster
  (:require [om.core :as om :include-macros true]
            [shodan.console :as console :include-macros true]
            [sablono.core :as s :include-macros true]
            [openslack.routing :refer [navigate contact-route]]
            [openslack.state :as st]))

(defn roster
  [state owner]
  (reify
    om/IDisplayName
    (display-name [_] "sidebar-roster")

    om/IRender
    (render [_]
      (let [roster (:roster state)
            contacts (->> (seq roster)
                          (sort-by first)
                          (map second))]
        (when (seq contacts)
          (s/html
           [:div.room-list.sidebar-list
            [:h3 "Contact List"]
            [:ul
             (for [contact contacts]
               (let [presence (st/get-presence contact)
                     name (:local contact)
                     is-current-contact? (and (= (get-in state [:page :state]) :contact)
                                              (= (get-in state [:page :contact]) name))
                     attrs {:on-click #(navigate (contact-route {:name name}))
                            :class-name (when is-current-contact? "highlighted")}
                     availability (:availability presence)
                     status (:status presence)]
                 [:li attrs
                  (condp = availability
                    :available [:span.status.online]
                    :unavailable [:span.status.offline]
                    :dnd [:span.status.busy]
                    [:span.status.offline])
                  name
                  [:div.read-status status]])
               [:li.add [:span "+"] "Add new contact\n              "])]]))))))
