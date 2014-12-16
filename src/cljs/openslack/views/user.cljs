(ns openslack.views.user
  (:require [om.core :as om :include-macros true]
            [sablono.core :as s :include-macros true]))

(defn user [state owner]
  (reify
    om/IDisplayName
    (display-name [_] "User")

    om/IRender
    (render [_]
      (when-let [user (:user state)]
        (s/html
         [:div.active-user
          ; TODO: avatar
          [:img
           {:height "50",
            :width "50",
            :alt "#user",
            :src "/static/imgs/placerholder-avatar-1.jpg"}]
          [:div.square
           [:div.row [:h2 (:local user)]]
           (let [status-text [:p.status-text (:status user "Put your status text here")]]
             (condp = (:availability user :available)
               :available [:div.row [:div.status.online] status-text]
               :offline [:div.row [:div.status.offline] status-text]
               :dnd [:div.row [:div.status.busy] status-text]))]])))))
