(ns sloth.views.home
  (:require [om.core :as om :include-macros true]
            [sablono.core :as s :refer-macros [html]]
            [sloth.routing :refer [static-image-route]]))

(defn home-component
  [_ owner]
  (reify
    om/IDisplayName
    (display-name [_] "home")

    om/IRender
    (render [_]
      (s/html
       [:div.sloth-main
        [:div.sloth-overflow.bounceInUp
         [:div.sloth-presentation
          [:div.text
           [:h1 "Welcome to Sloth"]
           [:h2
            "My name is Slotherin and I'm going to explain some cool features about Sloth"]
           [:p
            "· Sloth brings all your team communication into one place, makes it all instantly searchable and available wherever you go."]
           [:p
            "· Our aim is to make your working life simpler, more pleasant and more productive."]
           [:p
            "· Communication  in sloth happens in public channels, direct messages and private groups."]
           [:p
            "· Everything is indexed, archived and synced across devices so you can always pick up exactly where you left off."]
           [:p.last
            "\n              When you're ready, "
            [:a {:href "#"} "invite your teammates to sloth"]
            ".\n            "]]
          [:img {:src (static-image-route "slotherin.png")}]]]]))))
