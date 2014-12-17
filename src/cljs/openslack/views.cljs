(ns openslack.views
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [openslack.state :as st]
            [openslack.views.login :refer [login]]
            [openslack.views.sidebar :refer [sidebar]]
            [openslack.views.room :refer [room]]
            [openslack.views.contact :refer [contact]]))

(def home [:section.client-main])

(defn app [state owner]
  (reify
    om/IDisplayName
    (display-name [_] "Sloth")

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
