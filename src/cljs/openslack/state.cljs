(ns openslack.state)

; TODO: schema of state?
(defn make-initial-state []
  {:page {:name :login}
   :client nil
   :features []
   :roster []
   :user nil
   :channels [
              {:name "sloth"
               :unread 3}
              {:name "general"
               :unread 2}
              {:name "random"
               :unread 0}
              ]
   :conversations {:chat {}, :groupchat {}}})

(def state (atom (make-initial-state)))

(defn initialize-session
  [{:keys [user client]}]
  (swap! state assoc :client client)
  (swap! state assoc :user user))

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
