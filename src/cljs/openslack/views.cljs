(ns openslack.views
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [openslack.views.roster :refer [roster]]
            [openslack.views.user :refer [user]]))

(defn app [state owner]
  (reify
    om/IDisplayName
    (display-name [_] "Sloth")

    om/IRender
    (render [_]
      (html [:h1 "Sloth"
             [:section (om/build user state)]
             [:section (om/build roster state)]]))))
