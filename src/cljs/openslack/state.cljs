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

(defonce state (atom (make-initial-state)))

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
        from (get-in chat [:from :bare])]
    (update-in app-state [:conversations type from] (fnil conj []) chat)))

(defn add-own-chat
  [app-state chat]
  (update-in app-state [:conversations :chat (:to chat)] (fnil conj []) chat))

(defn join-room
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
  [app-state user]
  (get-in app-state [:presence (get-in user [:jid :full])]))

(defn room
  [app-state name]
  (first (filter #(= name (get-in % [:jid :local]))
                 (:channels app-state))))

(defn contact
  [app-state name]
  (first (filter #(= name (get-in % [:jid :local]))
                 (:roster app-state))))

(defn room-messages
  [app-state room]
  (get-in app-state [:conversations :groupchat (get-in room [:jid :bare])]))

(defn contact-messages
  [app-state user]
  (get-in app-state [:conversations :chat (get-in user [:jid :bare])]))
