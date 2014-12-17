(ns openslack.views.contact
  (:require [om.core :as om :include-macros true]
            [sablono.core :as s :include-macros true]
            [shodan.console :as console :include-macros true]
            [cuerdas.core :as str]
            [openslack.state :as st]
            [openslack.views.text :refer [enrich-text]]
            [openslack.chat :as chat]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Message input
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn send-message
  [owner user message]
  (let [useraddress (get-in user [:jid :bare])
        message (str/trim message)]
    (console/log "SEND TO" useraddress)
    (console/log "SEND MSG" message)
    (chat/send-personal-message useraddress message)
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
  [state owner user event]
  (let [target (.-target event)
        message (.-value target)]
    (cond
     (str/empty? message)
     (.preventDefault event)

     (ready-to-send? event message)
     (do
       (.preventDefault event)
       (send-message owner user message)
       (set!  (.-value target) ""))

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

        (console/log (pr-str nickname)
                     (pr-str user))
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
    (display-name [_] "contact-message")

    om/IInitState
    (init-state [_]
      (let [nickname (get-in state [:page :contact])
            user (st/get-contact state nickname)
            author (:author event)
            loggeduser (:user state)
            classname (if (= author (:local loggeduser))
                        "message self"
                        "message")]
        {:author (:resource (:from event))
         :body (enrich-text (:body event))
         :avatar (:avatar event)
         :classname classname}))

    om/IRenderState
    (render-state [_ {:keys [author classname avatar body]}]
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
;; Contact chat
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn contact
  [state owner]
  (reify
    om/IDisplayName
    (display-name [_] "contact")

    om/IInitState
    (init-state [_]
      {:nickname (get-in state [:page :contact])})

    om/IRenderState
    (render-state [_ {:keys [nickname]}]
      (when nickname
        (let [user (st/get-contact state nickname)
              presence (st/get-presence state user)
              status (:status presence)
              messages (st/get-contact-messages state user)]
          (s/html
           [:section.client-main
            [:header
             [:h1 (str "@" nickname)]
             [:h2 status ]]
            [:hr]
            [:div.chat-zone
             [:div.chat-container
              [:div.messages-container
               (for [msg messages]
                 (om/build message state {:opts msg
                                          :react-key (:id msg)}))]
              (om/build message-input state)]
             [:div.chat-sidebar-holder [:div]]]]))))))
