(ns openslack.views.room
  (:require [om.core :as om :include-macros true]
            [sablono.core :as s :include-macros true]
            [shodan.console :as console :include-macros true]
            [cuerdas.core :as str]
            [openslack.state :as st]
            [openslack.browser :as browser]
            [openslack.views.messages :as msg]
            [openslack.text :refer [enrich-text]]
            [openslack.chat :as chat]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Message input
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn send-message
  [owner room message]
  (let [roomaddress (get-in room [:jid :bare])]
    (chat/send-group-message roomaddress message)
    (om/set-state! owner :message "")))

(defn- ready-to-send?
  [event message]
  (if (= (.-keyCode event) 13)
    (if (or (.-ctrlKey event) (.-shiftKey event))
      false
      true)
    false))

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
       (send-message owner room message)
       (set!  (.-value target) ""))

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

(defn message
  [state owner event]
  (reify
    om/IDisplayName
    (display-name [_] "room-message")

    om/IInitState
    (init-state [_]
      (let [roomname (get-in state [:page :room])
            room (st/get-room state roomname)
            author (:author event)
            loggeduser (:user state)
            classname (if (= author (:local loggeduser))
                        "message self"
                        "message")]
        {:room room
         :author (:resource (:from event))
         :body (enrich-text (:body event))
         :avatar (:avatar event)
         :classname classname}))

    om/IRenderState
    (render-state [_ {:keys [room author classname avatar body]}]
      (let [stamp (:timestamp event)
            hours (.getHours stamp)
            mins (.getMinutes stamp)
            mins (if (< mins 10) (str "0" mins) mins)]
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

    om/IDidUpdate
    (did-update [this _ _]
      (browser/scroll-to-bottom ".messages-container"))

    om/IRender
    (render [_]
      (let [roomname (get-in state [:page :room])
            room (st/get-room state roomname)
            messages (st/get-room-messages state room)]
        (when room
          (s/html
           [:section.client-main
            [:header
             [:h1 (str "#" (get-in room [:jid :local]))]
             [:h2
              "Le topic del dia: Los "
              [:strong "Sloth"]
              " dominaran el mundo\n        "]]
            [:hr]
            [:div.chat-zone
             [:div.chat-container
              [:div.messages-container
               (for [msg messages]
                 (om/build message state {:opts msg
                                          :react-key (:id msg)}))]
              (om/build message-input state)]
             [:div.chat-sidebar-holder [:div]]]]))))))
