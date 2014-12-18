(ns openslack.views.channels
  (:require [om.core :as om :include-macros true]
            [openslack.routing :refer [navigate room-route]]
            [shodan.console :as console :include-macros true]
            [sablono.core :as s :include-macros true]
            [openslack.views.user :refer [user]]))

(defn channels
  [state owner]
  (reify
    om/IDisplayName
    (display-name [_] "Channels")

    om/IInitState
    (init-state [_]
      (let [channels (:channels state)
            chanlist (sort-by first (seq channels))
            chanlist (map second chanlist)]
        {:channels channels
         :chanlist chanlist}))

    om/IRenderState
    (render-state [_ {:keys [channels chanlist]}]
      (when chanlist
        (s/html
         [:div.room-list.sidebar-list
          [:h3 "Channels"]
          [:ul (for [chan chanlist]
                 (let [name (:local chan)
                       is-current-chan? (and (= (get-in state [:page :state]) :room)
                                             (= (get-in state [:page :room])  name))
                       unread (:unread chan 0)
                       attrs {:on-click #(navigate (room-route {:name name}))
                              :class-name (when is-current-chan? "highlighted")}
                       hashname (str "#" name)]
                   (if (> unread 0)
                     [:li.unread attrs hashname [:i unread] [:i.close-channel "x"]]
                     [:li attrs hashname [:i unread] [:i.close-channel "x"]])))
           [:li.add
            [:span "+"] "Add new channel\n              "]]])))))
