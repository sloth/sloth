(ns openslack.views.channels
  (:require [om.core :as om :include-macros true]
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
                        :let [name (:name chan)
                              unread (:unread chan 0)]]
                    (if (> unread 0)
                      [:li.unread [:span "#"] name [:i unread]]
                      [:li [:span "#"] name]
                      ))]])))))
