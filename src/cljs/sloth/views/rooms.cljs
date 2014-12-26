(ns sloth.views.rooms
  (:require [om.core :as om :include-macros true]
            [sloth.routing :refer [navigate room-route]]
            [shodan.console :as console :include-macros true]
            [sablono.core :as s :include-macros true]
            [sloth.views.user :refer [user]]))


(defn- is-current-room?
  [state name]
  (and (= (get-in state [:page :state]) :room)
       (= (get-in state [:page :room])  name)))

(defn room-component
  [state owner {:keys [room]}]
  (reify
    om/IDisplayName
    (display-name [_] "room-item")

    om/IRender
    (render [_]
      (let [name (get-in room [:jid :local])
            current (is-current-room? state name)
            unread (get room :unread 0)
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

(defn rooms
  [state owner]
  (reify
    om/IDisplayName
    (display-name [_] "rooms")

    om/IRender
    (render [_]
      (let [roomlist (vals (:rooms state))]
        (when roomlist
          (s/html
           [:div.room-list.sidebar-list
            [:h3 "Rooms"]
            [:ul (for [room roomlist]
                   (om/build room-component state {:opts {:room room}
                                                      :key :local}))
             [:li.add
              [:span "+"] "Add new room\n              "]]]))))))
