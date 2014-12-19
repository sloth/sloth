(ns sloth.state
  (:require [om.core :as om :include-macros true]
            [shodan.console :as console :include-macros true]))

; TODO: schema of state?
(defn make-initial-state []
  {:page {:state :login}
   :client nil
   :user nil
   :features []
   :roster {}
   :presence {}
   :subscriptions []
   :window-focus :focus
   ;; :subscriptions [{:type :room
   ;;                  :from {:jid "niwi@niwi.be"
   ;;                         :local "niwi"}
   ;;                  :room {:name "clojure"}}
   ;;                 {:type :room
   ;;                  :from {:jid "miguel@niwi.be"
   ;;                         :local "miguel"}
   ;;                  :room {:name "emacs"}}
   ;;                 {:type :room
   ;;                  :from {:jid "ramiro@niwi.be"
   ;;                         :local "ramiro"}
   ;;                  :room {:name "anime"}}]
   :channels {}
   :chats {}
   :groupchats {}})

(defonce state (atom nil))

(defn set-initial-state
  ([] (reset! state (make-initial-state)))
  ([s]
   (let [initial (make-initial-state)]
     (reset! state (merge initial s)))))

(defn logged-in?
  ([] (logged-in? @state))
  ([st]
   (not (nil? (:auth st)))))

(defn get-logged-user
  ([] (get-logged-user @state))
  ([state] (:user state)))

(defn get-client
  ([] (get-client @state))
  ([state] (:client state)))

(defn initialize-session
  [{:keys [user client auth]}]
  (swap! state (fn [v]
                 (-> v
                     (assoc :client client)
                     (assoc :user user)
                     (assoc :auth auth)))))

(defn add-chat
  [app-state chat]
  (let [type (:type chat)
        from (get-in chat [:from :bare])]
    (update-in app-state [ type from] (fnil conj []) chat)))

(declare get-room)
(declare get-current-room)

(defn clear-room-unread-messages
  [room]
  (let [roomname (:local room)]
    (swap! state
           (fn [state]
             (update-in state [:channels (keyword roomname)] assoc :unread 0)))))

(defn insert-group-message
  [message]
  (let [roomname (get-in message [:from :local])
        recipient (get-in message [:from :bare])
        currentroom (get-current-room)]

    (when (and (not (:delay message))
               (not= (:local currentroom) roomname))
      (when-let [room (get-room roomname)]
        (swap! state (fn [state]
                       (update-in state [:channels (keyword roomname) :unread] inc)))))

    ;; Insert message to state
    (swap! state (fn [state]
                   (update-in state [:groupchats recipient] (fnil conj []) message)))))

(defn insert-private-message
  [recipient message]
  (let [recipient (get recipient :bare)]
    ;; Append messages
    (swap! state (fn [state]
                   (update-in state [:chats recipient] (fnil conj []) message)))))

(defn join-room
  ;; TODO: rename
  [app-state room]
  (let [type :groupchat
        from (-> room :from :bare)]
    (update-in app-state [:conversations type from] (fnil empty []))))

(defn update-own-presence
  [app-state presence]
  (if (and (= (get-in app-state [:user :resource])
              (get-in presence [:from :resource])))
    (assoc-in app-state [:presence (get-in presence [:from :bare])] {:availability (:type presence)
                                                                     :status (:status presence)})
    app-state))

(defn update-presence
  [app-state presence]
  (if (and (= (get-in app-state [:user :local])
              (get-in presence [:from :local])))
    (update-own-presence app-state presence)
    (assoc-in app-state [:presence (get-in presence [:from :bare])] {:availability (:type presence)
                                                                     :status (:status presence)})))
(defn get-presence
  ([user] (get-presence @state user))
  ([state user]
   (let [useraddress (get-in user [:full])]
     (get-in state [:presence useraddress]))))

(defn get-room
  ([name] (get-room @state name))
  ([state name]
   (let [channels (:channels state)
         name (keyword name)]
     (get channels name))))

(defn get-current-room
  ([] (get-current-room @state))
  ([state]
   (when-let [roomname (get-in state [:page :room])]
     (get-room state roomname))))

(defn update-roster
  [roster]
  (swap! state assoc :roster roster))

(defn get-contact
  ([nickname] (get-contact @state nickname))
  ([state nickname]
   (let [contacts (:roster state)]
     (get contacts (keyword nickname)))))

(defn get-room-messages
  [state room]
  (let [roomaddress (:bare room)]
    (get-in state [:groupchats roomaddress])))

(defn get-contact-messages
  [state user]
  (let [useraddress (:bare user)]
    (get-in state [:chats useraddress])))

(defn window-focused?
  []
  (= :focus (:window-focus @state)))
