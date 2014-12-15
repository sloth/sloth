(ns openslack.views.sidebar
  (:require [om.core :as om :include-macros true]
            [sablono.core :as s :include-macros true]
            [openslack.views.user :refer [user]]
            [openslack.views.channels :refer [channels]]))

(defn sidebar
  [state owner]
  (reify
    om/IDisplayName
    (display-name [_] "Sidebar")

    om/IRender
    (render [_]
      (s/html [:div.client-sidebar
               [:div.logo "SlothLogo"]
               (om/build user state)
               (om/build channels state)
               [:hr]
               [:div.room-list.sidebar-list
                [:h3.nohover "Invited to this channels"]
                [:ul
                 [:li.invited
                  [:span "#"]
                  "SlothOnElorrio"
                  [:i "!"]
                  [:p "By " [:a {:href "#"} "@dialelo"]]]
                 [:li.invited
                  [:span "#"]
                  "SlothAreBadass"
                  [:i "!"]
                  [:p "By " [:a {:href "#"} "@mgdelacroix"]]]
                 [:li.invited
                  [:span "#"]
                  "SlothNucelarSurvivor"
                  [:i "!"]
                  [:p "By " [:a {:href "#"} "@rsanchezbalo"]]]
                 [:li.invited
                  [:span "#"]
                  "SlothSlacker"
                  [:i "!"]
                  [:p "By " [:a {:href "#"} "@niwibe"]]]]]])
      )))
