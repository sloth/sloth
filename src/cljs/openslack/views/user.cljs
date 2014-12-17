(ns openslack.views.user
  (:require [om.core :as om :include-macros true]
            [sablono.core :as s :include-macros true]
            [openslack.state :as st]))

(def availability->default-status
  {:available "Online"
   :unavailable "Offline"
   :dnd "Busy"
   :xa "Busy"})

(defn user [state owner]
  (reify
    om/IDisplayName
    (display-name [_] "User")

    om/IRender
    (render [_]
      (when (:user state)
        (let [jid (:user state)
              presence (st/get-presence @st/state {:jid jid})]
          (s/html
           [:div.active-user
            ;; TODO: avatar
            [:img
             {:height "50",
              :width "50",
              :alt "#user",
              :src "/static/imgs/placerholder-avatar-1.jpg"}]
            [:div.square
             [:div.row [:h2 (:local jid)]]
             (let [availability (:availability presence :available)
                   default-status (availability->default-status availability)
                   status (:status presence)
                   status-text [:p.status-text (if status status default-status)]]
               (condp = availability
                 :available [:div.row [:div.status.online] status-text]
                 :unavailable [:div.row [:div.status.offline] status-text]
                 :dnd [:div.row [:div.status.busy] status-text]))]]))))))
