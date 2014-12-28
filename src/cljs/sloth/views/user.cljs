(ns sloth.views.user
  (:require [om.core :as om :include-macros true]
            [sablono.core :as s :include-macros true]
            [cuerdas.core :as str]
            [sloth.events :as events]
            [sloth.chat :as chat]
            [sloth.types :as types]
            [sloth.state :as st]))

(def availability->default-status
  {:available "Online"
   :unavailable "Offline"
   :dnd "Busy"
   :xa "Busy"})

(defn default-status?
  [status]
  (let [default-statuses (set (vals availability->default-status))]
    (default-statuses status)))

(defn- clean-status
  [raw-status]
  (-> raw-status
      str/strip-tags
      str/trim))

(defn on-blur
  [state event]
  (let [target (.-target event)
        status-text (clean-status (.-innerHTML target))]
    (if-not (and (str/empty? status-text)
                 (default-status? status-text))
      (chat/set-status state status-text)
      (let [availability (get-in state [:user-presence :availability])
            default-status (availability->default-status availability)]
        (chat/set-status state "")
        (set! (.-innerHTML target) default-status)))))

(defn on-enter
  [state event]
  (let [target (.-target event)
        status-text (clean-status (.-innerHTML target))]
    (set! (.-innerHTML target) status-text)
    (.blur target)))

(defn on-key-up
  [state event]
  (when (events/pressed-enter? event)
    (on-enter state event)))

(defn user
  [state owner]
  (reify
    om/IDisplayName
    (display-name [_] "user")

    om/IRender
    (render [_]
      (let [user (:user state)]
        (when-let [presence (some->> user (st/get-presence state))]
          (s/html
           [:div.active-user
            ;; TODO: avatar
            [:img {:height "50",
                   :width "50",
                   :alt "#user",
                   :src "static/imgs/placerholder-avatar-1.jpg"}]
            [:div.square
             [:div.row [:h2 (types/get-user-local user)]]
             (let [availability (:availability presence :available)
                   default-status (availability->default-status availability)
                   status (get presence :status "")
                   status-text [:p.status-text
                                {:content-editable true
                                 :on-key-up (partial on-key-up state)
                                 :on-blur (partial on-blur state)}
                                (if (or (nil? status)
                                        (str/empty? status))
                                  default-status
                                  status)]]
               (condp = availability
                 :available [:div.row
                             [:div.status.online]
                             status-text]
                 :unavailable [:div.row
                               [:div.status.offline]
                               status-text]
                 :dnd [:div.row
                       [:div.status.busy]
                       status-text]))]]))))))
