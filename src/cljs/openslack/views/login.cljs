(ns openslack.views.login
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

(defn login
  [state owner]
  (reify
    om/IDisplayName
    (display-name [_] "Login")

    om/IRender
    (render [_]
      (when (:roster state)
        (html [:form
               [:label "Username"]
               [:input {:type :text :value "User"}]
               [:label "Password"]
               [:input {:type :password :value ""}]])))))
