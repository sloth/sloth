(ns openslack.views.room
  (:require [om.core :as om :include-macros true]
            [sablono.core :as s :include-macros true]
            [shodan.console :as console :include-macros true]
            [cuerdas.core :as str]
            [openslack.routing :refer [placeholder-avatar-route]]
            [openslack.state :as st]
            [openslack.browser :as browser]
            [openslack.views.text :refer [enrich-text]]
            [openslack.chat :as chat]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Message input
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- ready-to-send?
  [event message]
  (and (= (.-keyCode event) 13)
       (not (or (.-ctrlKey event) (.-shiftKey event)))
       (not (str/empty? message))))

(defn- onkeyup
  [state owner room event]
  (let [target (.-target event)
        message (.-value target)]
    (cond
     (str/empty? message)
     (.preventDefault event)

     (ready-to-send? event message)
     (do
       (.preventDefault event)
       (chat/send-group-message state room message)
       (om/set-state! owner :message "")
       (set! (.-value target) ""))

     :else
     (om/set-state! owner :message message))))

(defn message-input
  [state owner]
  (reify
    om/IDisplayName
    (display-name [_] "room-input")

    om/IInitState
    (init-state [_] {:message ""})

    om/IRenderState
    (render-state [_ {:keys [message]}]
      (let [roomname (get-in state [:page :room])
            room (st/get-room state roomname)
            onkeyup (partial onkeyup state owner room)]
        (s/html
         [:div.write-message
          [:textarea {:auto-focus true
                      :on-key-up onkeyup}]])))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Messages
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn message-component
  [state owner {:keys [message room loggeduser]}]
  (reify
    om/IDisplayName
    (display-name [_] "room-message")

    om/IRender
    (render [_]
      (let [author (get-in message [:from :resource])
            loggeduser (:user state)
            classname (if (= author (:local loggeduser))
                        "message self"
                        "message")
            avatar (:avatar message placeholder-avatar-route)
            body (enrich-text (:body message))
            stamp (:timestamp message)
            hours (.getHours stamp)
            mins (let [mins (.getMinutes stamp)]
                   (if (< mins 10) (str "0" mins) mins))]
        (s/html
         [:div {:class-name classname}
          [:div.message-avatar
           [:img {:height "35"
                  :width "35"
                  :alt "#user"
                  :src avatar}]]
          [:div.message-content
           [:div.message-title
            [:strong author]
            [:span (str hours ":" mins)]]
           [:p.content body]]])))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Room
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn room
  [state owner]
  (reify
    om/IDisplayName
    (display-name [_] "Room")

    om/IDidMount
    (did-mount [_]
      (browser/scroll-to-bottom ".messages-container"))

    om/IDidUpdate
    (did-update [this _ _]
      (browser/scroll-to-bottom ".messages-container"))

    om/IRender
    (render [_]
      (let [roomname (get-in state [:page :room])
            room (st/get-room state roomname)
            loggeduser (:user state)
            messages (st/get-room-messages state room)]
        (when room
          (s/html
           [:section.client-main
            [:header
             [:h1 (str "#" (get-in room [:local]))]
             [:h2
              "Le topic del dia: Los "
              [:strong "Sloth"]
              " dominaran el mundo\n        "]]
            [:div.chat-zone
             [:div.chat-container
              [:div.messages-container
               (for [msg messages]
                 (om/build message-component state {:opts {:message msg
                                                           :room room
                                                           :loggeduser loggeduser}
                                          :react-key (:id msg)}))]
              (om/build message-input state)]
             [:div.chat-sidebar-holder [:div]]]]))))))
