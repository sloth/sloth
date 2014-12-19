(ns sloth.views.channels
  (:require [om.core :as om :include-macros true]
            [sloth.routing :refer [navigate room-route]]
            [shodan.console :as console :include-macros true]
            [sablono.core :as s :include-macros true]
            [sloth.views.user :refer [user]]))


(defn- is-current-chan?
  [state name]
  (and (= (get-in state [:page :state]) :room)
       (= (get-in state [:page :room])  name)))

(defn channel-component
  [state owner {:keys [channel]}]
  (reify
    om/IDisplayName
    (display-name [_] "channel-item")

    om/IRender
    (render [_]
      (let [name (:local channel)
            current (is-current-chan? state name)
            unread (get channel :unread 0)
            attrs {:on-click #(navigate (room-route {:name name}))
                   :class-name (cond
                                (and current (> unread 0)) "highlighted unread"
                                (and current (= unread 0)) "highlighted"
                                (> unread 0) "unread"
                                :else "")}]
        (s/html
         (if (> unread 0)
           [:li attrs [:span "#"] name [:i unread] [:i.close-channel "x"]]
           [:li attrs [:span "#"] name [:i.close-channel "x"]]))))))

(defn channels
  [state owner]
  (reify
    om/IDisplayName
    (display-name [_] "channels")

    om/IRender
    (render [_]
      (let [channels (:channels state)
            chanlist (sort-by first (seq channels))
            chanlist (map second chanlist)]
        (when chanlist
          (s/html
           [:div.room-list.sidebar-list
            [:h3 "Channels"]
            [:ul (for [chan chanlist]
                   (om/build channel-component state {:opts {:channel chan}
                                                      :key :local}))
             [:li.add
              [:span "+"] "Add new channel\n              "]]]))))))
