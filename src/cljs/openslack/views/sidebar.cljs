(ns openslack.views.sidebar
  (:require [om.core :as om :include-macros true]
            [sablono.core :as s :include-macros true]
            [openslack.views.user :refer [user]]))

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
               [:div.room-list.sidebar-list
                [:h3 "Channels"]
                [:ul
                 [:li.unread [:span "#"] "SlothMyMachine" [:i "3"]]
                 [:li.unread [:span "#"] "SlothThisShit" [:i "4"]]
                 [:li [:span "#"] "SlothThugLife\n            "]]]
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
