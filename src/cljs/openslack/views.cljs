(ns openslack.views
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

(defn roster [state owner]
  (reify
    om/IDisplayName
    (display-name [_] "Roster")

    om/IRender
    (render [_]
      (when (:roster state)
        (html [:h2 "Roster"
               [:ul
                (for [user (:roster state)]
                  [:li (-> user :jid :local)])]])))))

(defn user [state owner]
  (reify
    om/IDisplayName
    (display-name [_] "User")

    om/IRender
    (render [_]
      (when (:user state)
        (html [:h2 (-> state :user :local)])))))

(defn app [state owner]
  (reify
    om/IDisplayName
    (display-name [_] "Open Slack")

    om/IRender
    (render [_]
      (html [:h1 "Open Sloth"
             [:section (om/build user state)]
             [:section (om/build roster state)]]))))
