(ns sloth.views.sidebar.invitations
  (:require [om.core :as om :include-macros true]
            [sablono.core :as s :include-macros true]
            [shodan.console :as console :include-macros true]
            [sloth.routing :as routing]
            [sloth.types :as types]
            [sloth.chat :as chat]))

(defn on-click
  [room event]
  (.preventDefault event)
;; TODO
;;  (chat/accept-room-invitation room)
)

(defn- invitation-component
  [state owner {:keys [invitation]}]
  (reify
    om/IDisplayName
    (display-name [_] "sidebar-invitations")

    om/IInitState
    (init-state [_]
      (let [from (get invitation :from)
            from-name (types/get-user-local from)
            room-name (get-in invitation [:room :local])
            route (routing/contact-route {:name from-name})]
        {:route route
         :from from
         :from-name from-name
         :room-name room-name}))

    om/IRenderState
    (render-state [_ {:keys [route from-name room-name]}]
      [:li.invited
       [:span "#"] room-name
       [:i "!"]
       [:p "By " [:a {:href route
                      :on-click #(routing/navigate route)}
                  (str "@" from-name)]]])))

(defn invitations-component
  [state owner]
  (reify
    om/IDisplayName
    (display-name [_] "sidebar-invitations")

    om/IRender
    (render [_]
      (when-let [invitations (seq (:room-invitations state))]
        (s/html
         [:div.room-list.sidebar-list
          [:h3.nohover "Invited to this rooms"]
          [:ul
           (for [item invitations]
             (om/build invitation-component state
                       {:opts {:invitation item}
                        :react-key (str (gensym 'sloth))}))]])))))
