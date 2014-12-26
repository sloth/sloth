(ns sloth.state
  (:require [om.core :as om :include-macros true]
            [cuerdas.core :as str]
            [shodan.console :as console :include-macros true]))

; TODO: schema of state?
(defn make-initial-state []
  {:page {:state :login}
   :client nil
   :user nil
   :user-presence {}
   :features []
   :roster {}
   :presence {}
   :subscriptions []
   :room-invitations []
   :window-focus :focus
   :channels {:sloth {:local "sloth"
                      :bare "sloth@conference.niwi.be"
                      :unread 0}
              :demo {:local "demo"
                     :bare "demo@conference.niwi.be"
                     :unread 0}}
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

(defn clear-room-unread-messages
  [room]
  (let [roomname (:local room)]
    (swap! state
           (fn [state]
             (update-in state [:channels (keyword roomname)] assoc :unread 0)))))

(defn set-room-subject
  [room subject]
  (let [roomkey (keyword (:local room))]
    (swap! state (fn [st]
                   (assoc-in st [:channels roomkey :subject] subject)))))

(defn add-room-invitation
  [invitation]
  (swap! state (fn [st]
                 (update-in st [:room-invitations] conj invitation))))

(defn insert-group-message
  [message]
  (let [room (:from message)
        roomname (:local room)
        roomkey (keyword roomname)
        recipient (:bare room)
        currentroom (get-current-room)]
    (when (and (not (:delay message))
               (not= (:local currentroom) roomname))
      (when-let [room (get-room roomname)]
        (swap! state (fn [st]
                       (update-in st [:channels roomkey :unread] inc)))))

    (if (contains? message :subject)
      ;; Modify room subject
      (set-room-subject room (:subject message))
      ;; Insert message to state
      (swap! state (fn [st]
                     (update-in st [:groupchats recipient] (fnil conj []) message))))))

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
    (assoc app-state :user-presence (select-keys presence [:availability :status :priority]))
    app-state))

(defn update-others-presence
  [app-state {:keys [from] :as presence}]
  (let [bare (:bare from)
        resource (:resource from)]
    (assoc-in app-state [:presence bare resource] presence)))

(defn update-presence
  [app-state presence]
  (if (and (= (get-in app-state [:user :bare])
              (get-in presence [:from :bare])))
    (update-own-presence app-state presence)
    (update-others-presence app-state presence)))

(defn get-others-presence
  [app-state user]
  (let [resources (get-in app-state [:presence (:bare user)])
        presences (vals resources)]
    (first (sort-by :priority > presences))))

(defn get-presence
  ([user] (get-presence @state user))
  ([state user]
   (let [useraddress (:bare user)]
     (if (= useraddress (:bare (:user state)))
       (:user-presence state)
       (get-others-presence state user)))))

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

(def groupchat-entries-xform
  (comp
   (partition-by (fn [msg]
                   (get-in msg [:from :resource])))
   (map (fn [entries]
          {:messages entries
           :from (:from (first entries))}))))

(defn get-room-message-entries
  [state room]
  (sequence groupchat-entries-xform (get-room-messages state room)))

(defn get-contact-messages
  [state user]
  (let [useraddress (:bare user)]
    (get-in state [:chats useraddress])))

(def chat-entries-xform
  (comp
   (partition-by (fn [msg]
                   (get-in msg [:from :bare])))
   (map (fn [entries]
          {:messages entries
           :from (:from (first entries))}))))

(defn get-contact-message-entries
  [state user]
  (sequence chat-entries-xform (get-contact-messages state user)))

(defn window-focused?
  []
  (= :focus (:window-focus @state)))
