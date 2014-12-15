(ns openslack.views.roster
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

(defn roster
  [state owner]
  (reify
    om/IDisplayName
    (display-name [_] "Roster")

    om/IRender
    (render [_]
      (when (:roster state)
        (html [:h2 "Roster"
               [:ul
                (for [user (:roster state)]
                  [:li (get-in user [:jid :local])])]])))))
