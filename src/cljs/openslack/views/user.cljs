(ns openslack.views.user
  (:require [om.core :as om :include-macros true]
            [sablono.core :as s :include-macros true]
            [openslack.state :as st]))

(defn user [state owner]
  (reify
    om/IDisplayName
    (display-name [_] "User")

    om/IRender
    (render [_]
      (when (:user state)
        (let [user (:user state)
              presence (st/get-presence user)]
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
             (let [status-text [:p.status-text (:status presence)]]
               (condp = (:availability presence :available)
                 :available [:div.row [:div.status.online] status-text]
                 :unavailable [:div.row [:div.status.offline] status-text]
                 :dnd [:div.row [:div.status.busy] status-text]))]]))))))
