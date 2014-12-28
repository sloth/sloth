(ns sloth.views.sidebar
  (:require [om.core :as om :include-macros true]
            [sablono.core :as s :include-macros true]
            [sloth.views.sidebar.user :refer [loggeduser-component]]
            [sloth.views.sidebar.rooms :refer [roomlist-component]]
            [sloth.views.sidebar.invitations :refer [invitations-component]]
            [sloth.views.sidebar.roster :refer [roster-component]]))


(defn sidebar-component
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
          (om/build roomlist-component state)
          [:hr]
          (om/build invitations-component state)
          [:hr]
          (om/build roster-component state)]
         (om/build loggeduser-component state)]]))))

