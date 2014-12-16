(ns openslack.state)

; TODO: schema of state?
(defn make-initial-state []
  {:page {:state :login}
   :client nil
   :features []
   :roster []
   :presence {}
   :user nil
   :subscriptions [
    {:type :room
     :from {:jid "niwi@niwi.be"
            :local "niwi"}
     :room {:name "clojure"}}
    {:type :room
     :from {:jid "miguel@niwi.be"
            :local "miguel"}
     :room {:name "emacs"}}
    {:type :room
     :from {:jid "ramiro@niwi.be"
            :local "ramiro"}
     :room {:name "anime"}}
   ]
   :channels [
      {:jid {:local "sloth"
             :bare "sloth@conference.niwi.be"}
       :unread 0}
   ]
   :conversations {:chat {}, :groupchat {}}})

(def state (atom (make-initial-state)))

(defn logged-in?
  ([]
     (logged-in? @state))
  ([st]
     (not (nil? (:user st)))))

(defn initialize-session
  [{:keys [user client]}]
  (swap! state assoc :client client)
  (swap! state assoc :user user))

(defn add-chat
  [app-state chat]
  (let [type (:type chat)
        from (-> chat :from :local)]
    (update-in app-state [:conversations type from] (fnil conj []) chat)))

(defn join-room
  [app-state room]
  (let [type :groupchat
        from (-> room :from :bare)]
    (update-in app-state [:conversations type from] (fnil empty []))))

(defn update-presence
  [app-state presence]
  (assoc-in app-state [:presence (get-in presence [:from :bare])] {:availability (:type presence)
                                                                   :status (:status presence)}))

(defn get-presence
  [user]
  (get-in @state [:presence (get-in user [:jid :bare])]))

(defn room
  [name]
  (first (filter #(= name (get-in % [:jid :local]))
                 (:channels @state))))

(defn contact
  [name]
  (first (filter #(= name (get-in % [:jid :local]))
                 (:roster @state))))

(defn room-messages
  [room]
  (get-in @state [:conversations :groupchat (get-in room [:jid :local])]))

(defn contact-messages
  [user]
  (get-in @state [:conversations :chat (get-in user [:jid :local])]))
