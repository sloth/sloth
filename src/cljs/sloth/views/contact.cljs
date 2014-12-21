(ns sloth.views.contact
  (:require [om.core :as om :include-macros true]
            [sablono.core :as s :include-macros true]
            [shodan.console :as console :include-macros true]
            [cuerdas.core :as str]
            [sloth.routing :refer [placeholder-avatar-route]]
            [sloth.state :as st]
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
  [state owner user event]
  (let [target (.-target event)
        message (.-value target)]
    (cond
     (str/empty? message)
     (.preventDefault event)

     (ready-to-send? event message)
     (do
       (.preventDefault event)
       (chat/send-personal-message state user message)
       (om/set-state! owner :message "")
       (set! (.-value target) ""))

     :else
     (om/set-state! owner :message message))))

(defn message-input
  [state owner]
  (reify
    om/IDisplayName
    (display-name [_] "user-input")

    om/IInitState
    (init-state [_] {:message ""})

    om/IRenderState
    (render-state [_ {:keys [message]}]
      (let [nickname (get-in state [:page :contact])
            user (st/get-contact state nickname)
            onkeyup (partial onkeyup state owner user)]

        (s/html
         [:div.write-message
          [:textarea {:auto-focus true
                      :on-key-up onkeyup}]])))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Messages
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn entry-component
  [state owner {:keys [from messages]}]
  (reify
    om/IDisplayName
    (display-name [_] "contact-message")

    om/IInitState
    (init-state [_]
      (let [nickname (get-in state [:page :contact])
            user (st/get-contact state nickname)
            author (:local from)
            loggeduser (:user state)
            classname (if (= author (:local loggeduser))
                        "message self"
                        "message")]
        {:author author
         :bodies (map (comp enrich-text :body) messages)
         ;; TODO
         :avatar placeholder-avatar-route
         :classname classname}))

    om/IRenderState
    (render-state [_ {:keys [author classname avatar bodies]}]
      (let [stamp (:timestamp (first messages))
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
           (for [body bodies]
             [:p.content body])]])))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Contact chat
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn contact
  [state owner]
  (reify
    om/IDisplayName
    (display-name [_] "contact")

    om/IRender
    (render [_]
      (when-let [nickname (get-in state [:page :contact])]
        (let [user (st/get-contact state nickname)
              presence (st/get-presence state user)
              status (:status presence)
              entries (st/get-contact-message-entries state user)]
          (s/html
           [:section.client-main
            [:header
             [:h1 (str "@" nickname)]
             [:h2 status]]
            [:hr]
            [:div.chat-zone
             [:div.chat-container
              [:div.messages-container
               (for [entry entries]
                 (om/build entry-component
                           state
                           {:opts entry
                            :react-key (apply str (map :id (:messages entry)))}))]
              (om/build message-input state)]
             [:div.chat-sidebar-holder [:div]]]]))))))
