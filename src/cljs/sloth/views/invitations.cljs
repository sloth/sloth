(ns sloth.views.invitations
  (:require [om.core :as om :include-macros true]
            [sablono.core :as s :include-macros true]
            [sloth.routing :as routing]
            [sloth.chat :as chat]))

(defn on-click
  [room event]
  (.preventDefault event)
;; TODO
;;  (chat/accept-room-invitation room)
)

(defn room-invitations
  [state owner]
  (reify
    om/IDisplayName
    (display-name [_] "subscriptions")

    om/IRender
    (render [_]
      (when-let [chan-subs (:room-invitations state)]
        (s/html [:div.room-list.sidebar-list
                 [:h3.nohover "Invited to this rooms"]
                 [:ul
                  (for [sub chan-subs
                        :let [route (routing/contact-route {:name (get-in sub [:from :local])})]]
                    [:li.invited
                     [:span "#"]
                     (get-in sub [:room :local])
                     [:i "!"]
                     [:p "By " [:a {:href route
                                    :on-click #(routing/navigate route)}
                                (str "@" (get-in sub [:from :local]))]]])]])))))
