(ns sloth.views.room
  (:require [om.core :as om :include-macros true]
            [sablono.core :as s :include-macros true]
            [shodan.console :as console :include-macros true]
            [cuerdas.core :as str]
            [sloth.events :as events]
            [sloth.routing :refer [placeholder-avatar-route]]
            [sloth.state :as st]
            [sloth.browser :as browser]
            [sloth.views.text :refer [enrich-text]]
            [sloth.chat :as chat]))

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
       (chat/send-group-message state (:jid room) message)
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

(defn entry-component
  [state owner {:keys [entry room loggeduser]}]
  (reify
    om/IDisplayName
    (display-name [_] "room-messages")

    om/IRender
    (render [_]
      (let [{:keys [messages from]} entry
            author (:resource from)
            classname (if (= author (:local loggeduser))
                        "message self"
                        "message")
            ;; TODO: avatar
            avatar placeholder-avatar-route
            bodies (map (comp enrich-text :body) messages)
            ;; TODO: include stamp in every msg
            stamp (:timestamp (first messages))
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
           (for [body bodies]
             [:p.content body])]])))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Room subject
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn clean-subject
  [s]
  (-> s
      str/strip-tags
      str/trim))

(defn room-subject-on-blur
  [state event]
  (let [target (.-target event)
        subject-text (clean-subject (.-innerHTML target))]
    (chat/set-room-subject (get-in state [:jid :bare]) subject-text)))

(defn room-subject-on-enter
  [state event]
  (let [target (.-target event)
        subject-text (clean-subject (.-innerHTML target))]
    (set! (.-innerHTML target) subject-text)
    (.blur target)))

(defn room-subject-on-key-up
  [state event]
  (when (events/pressed-enter? event)
    (room-subject-on-enter state event)))

(defn room-subject
  [state owner]
  (reify
    om/IDisplayName
    (display-name [_] "room-subject")

    om/IRender
    (render [_]
      (s/html
       [:h2 {:content-editable true
             :on-key-up (partial room-subject-on-key-up state)
             :on-blur (partial room-subject-on-blur state)}
        (:subject state)]))))

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
            entries (st/get-room-message-entries state room)]
        (when room
          (s/html
           [:section.client-main
            [:header
             [:h1 (str "#" (get-in room [:jid :local]))]
             (om/build room-subject room)]
            [:div.chat-zone
             [:div.chat-container
              [:div.messages-container
               (for [entry entries]
                 (om/build entry-component state {:opts {:entry entry
                                                         :room room
                                                         :loggeduser loggeduser}
                                                    :react-key (apply str (map :id (:messages entry)))}))]
              (om/build message-input state)]
             [:div.chat-sidebar-holder [:div]]]]))))))
