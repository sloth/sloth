(ns sloth.views
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [shodan.console :as console :include-macros true]
            [sloth.state :as st]
            [sloth.views.login :refer [login-component]]
            [sloth.views.sidebar :refer [sidebar-component]]
            [sloth.views.home :refer [home-component]]
            [sloth.views.room :refer [room-component]]
            [sloth.views.contact :refer [contact-component]]))


(defn- login-page
  [state owner]
  (om/build login-component state))

(defn- home-page
  [state owner]
  [:section#app.client
   [:div.client-sidebar-holder (om/build sidebar-component state)]
   (om/build home-component state)])

(defn- room-page
  [state owner]
  (let [room-name (get-in state [:page :room])
        r (st/get-room state room-name)]
    [:section#app.client
     [:div.client-sidebar-holder (om/build sidebar-component state)]
     (om/build room-component state)]))

(defn- contact-page
  [state owner]
  (let [name (get-in state [:page :contact])]
    [:section#app.client
     [:div.client-sidebar-holder (om/build sidebar-component state)]
     (om/build contact-component state)]))

(defn app [state owner]
  (reify
    om/IDisplayName
    (display-name [_] "sloth")

    om/IRender
    (render [_]
      (let [page (get-in state [:page :state])]
        (html (case page
                :login (login-page state owner)
                :home (home-page state owner)
                :room (room-page state owner)
                :contact (contact-page state owner)
                nil))))))
