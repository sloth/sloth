(ns openslack.views.user
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

(defn user [state owner]
  (reify
    om/IDisplayName
    (display-name [_] "User")

    om/IRender
    (render [_]
      (when (:user state)
        (html [:h2 (-> state :user :local)])))))
