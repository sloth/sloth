(ns sloth.views
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [sloth.state :as st]
            [sloth.views.login :refer [login]]
            [sloth.views.sidebar :refer [sidebar]]
            [sloth.views.room :refer [room]]
            [sloth.views.contact :refer [contact]]))

(def home [:section.client-main])

(defn app [state owner]
  (reify
    om/IDisplayName
    (display-name [_] "sloth")

    om/IRender
    (render [_]
      (html (condp = (get-in state [:page :state])
              :login (om/build login state)
              :home [:section#app.client
                     [:div.client-sidebar-holder (om/build sidebar state)]
                     home]
              :room (let [room-name (get-in state [:page :room])
                          r (st/get-room @st/state room-name)]
                      [:section#app.client
                       [:div.client-sidebar-holder (om/build sidebar state)]
                       (om/build room state)])
              :contact (let [contact-name (get-in state [:page :contact])]
                         [:section#app.client
                          [:div.client-sidebar-holder (om/build sidebar state)]
                          (om/build contact state)])
              nil)))))
