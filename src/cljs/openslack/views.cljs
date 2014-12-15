(ns openslack.views
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [openslack.views.login :refer [login]]
            [openslack.views.roster :refer [roster]]
            [openslack.views.user :refer [user]]))

(defn app [state owner]
  (reify
    om/IDisplayName
    (display-name [_] "Sloth")

    om/IRender
    (render [_]
      (html [:h1 "Sloth"
             (condp = (get-in state [:page :name])
               :login (om/build login state)
               :home [:div
                      [:section (om/build user state)]
                      [:section (om/build roster state)]]
               :room [:div
                      [:section (om/build user state)]
                      [:section (om/build roster state)]]
               :contact [:div
                         [:section (om/build user state)]
                         [:section (om/build roster state)]]
               nil
               )]))))
