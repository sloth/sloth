(ns openslack.state)

; TODO: schema of state?
(defn make-initial-state []
  {:client nil
   :features []
   :roster []
   :user nil
   :conversations {:chat {}, :groupchat {}}})

(defn add-chat
  [chat app-state]
  (let [type (:type chat)
        from (-> chat :from :bare)]
    (update-in app-state [:conversations type from] (fnil conj []) chat)))

(defn join-room
  [room app-state]
  (let [type :groupchat
        from (-> room :from :bare)]
    (update-in app-state [:conversations type from] (fnil empty []))))

(defn update-chat-state
  [chat-state app-state]
  nil)

(defn update-roster
  [roster-update app-state]
  nil)

(defn add-subscription
  [subscription app-state]
  nil)

(defn remove-subscription
  [subscription app-state]
  nil)

; TODO: functions for updating state
