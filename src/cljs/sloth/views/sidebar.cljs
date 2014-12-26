(ns sloth.views.sidebar
  (:require [om.core :as om :include-macros true]
            [sablono.core :as s :include-macros true]
            [sloth.views.user :refer [user]]
            [sloth.views.rooms :refer [rooms]]
            [sloth.views.invitations :refer [room-invitations]]
            [sloth.views.roster :refer [roster]]))

(defn sidebar
  [state owner]
  (reify
    om/IDisplayName
    (display-name [_] "sidebar")

    om/IRender
    (render [_]
      (s/html
       [:div.client-sidebar-holder
        [:audio#notification-sound
         [:source {:src "static/sounds/Sloth.ogg" :type "audio/ogg"}]]
        [:div.client-sidebar
         [:div.client-lists
          [:div.logo
           [:img {:alt "Sloth logo", :width "100%", :src "static/imgs/logo.png"}]]
          (om/build rooms state)
          [:hr]
          (om/build room-invitations state)
          [:hr]
          (om/build roster state)]
         (om/build user state)]]))))
