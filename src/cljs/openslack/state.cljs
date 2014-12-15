(ns openslack.state)

; TODO: schema of state?
(defn make-initial-state []
  {:page {:name :login}
   :client nil
   :features []
   :roster []
   :user nil
   :conversations {:chat {}, :groupchat {}}})

(def state (atom (make-initial-state)))

(defn add-chat
  [app-state chat]
  (let [type (:type chat)
        from (-> chat :from :bare)]
    (update-in app-state [:conversations type from] (fnil conj []) chat)))

(defn join-room
  [app-state room]
  (let [type :groupchat
        from (-> room :from :bare)]
    (update-in app-state [:conversations type from] (fnil empty []))))

; TODO: functions for updating state
