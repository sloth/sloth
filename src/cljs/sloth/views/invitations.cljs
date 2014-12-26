(ns sloth.views.invitations
  (:require [om.core :as om :include-macros true]
            [sablono.core :as s :include-macros true]
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
                 [:h3.nohover "Invited to this channels"]
                 [:ul
                  (for [sub chan-subs]
                    [:li.invited
                     [:span "#"]
                     (get-in sub [:room :local])
                     [:i "!"]
                     [:p "By " [:a {:href "#"
                                    :on-click (partial on-click (:room sub))}
                                (str "@" (get-in sub [:from :local]))]]])]])))))
