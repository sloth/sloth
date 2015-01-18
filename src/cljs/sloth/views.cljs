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
  (console/log "login-page")
  (om/build login-component state))

(defn- home-page
  [state owner]
  (console/log "home-page")
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

(defn app
  [state owner]
  (reify
    om/IDisplayName
    (display-name [_] "sloth")

    om/IRender
    (render [_]
      (let [route (st/get-route)]
        (console/log "app$render" (pr-str route))
        (condp = (:name route)
          :login (login-page state owner)
          :home (home-page state owner)
          :room (room-page state owner)
          :contact (contact-page state owner)
          nil)))))

(defn mount-root-component
  "Mount the main om component."
  []
  (let [bodyel (js/document.querySelector "body")
        component (om/root app st/app-state {:target bodyel})]
    (add-watch st/app-state :changes #(om/refresh! component))))
