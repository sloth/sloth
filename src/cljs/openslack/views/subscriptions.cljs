(ns openslack.views.subscriptions
  (:require [om.core :as om :include-macros true]
            [sablono.core :as s :include-macros true]))

(defn subscriptions
  [state owner]
  (reify
    om/IDisplayName
    (display-name [_] "Subscriptions")

    om/IRender
    (render [_]
      (when-let [chan-subs (filter #(= :room (:type %)) (:subscriptions state))]
        (s/html [:div.room-list.sidebar-list
                 [:h3.nohover "Invited to this channels"]
                 [:ul
                  (for [sub chan-subs]
                    [:li.invited
                     [:span "#"]
                     (-> sub :room :name)
                     [:i "!"]
                     [:p "By " [:a {:href "#"} (str "@" (-> sub :from :local))]]])]])))))
