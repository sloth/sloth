(ns openslack.views.channels
  (:require [om.core :as om :include-macros true]
            [openslack.routing :refer [navigate room-route]]
            [sablono.core :as s :include-macros true]
            [openslack.views.user :refer [user]]))

(defn channels
  [state owner]
  (reify
    om/IDisplayName
    (display-name [_] "Channels")

    om/IRender
    (render [_]
      (s/html (when (:channels state)
                [:div.room-list.sidebar-list
                 [:h3 "Channels"]
                 [:ul
                  (for [chan (:channels state)
                        :let [jid (:jid chan)
                              name (:local jid)
                              is-current-chan? (and (= (get-in state [:page :state]) :room)
                                                    (= (get-in state [:page :room])  name))
                              unread (:unread chan 0)
                              attrs {:on-click #(navigate (room-route {:name name}))
                                     :class-name (when is-current-chan? "highlight")}]]
                    (if (> unread 0)
                      [:li.unread attrs [:span "#"] name [:i unread]]
                      [:li attrs [:span "#"] name]
                      ))]])))))
