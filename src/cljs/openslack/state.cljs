(ns openslack.state)

; TODO: schema of state?
(defn make-initial-state []
  {:page {:state :login}
   :client nil
   :user nil
   :features []
   :roster []
   :presence {}
   :subscriptions []
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
   ;; :channels []
   :channels [
      {:jid {:local "sloth"
             :bare "sloth@conference.niwi.be"}
       :unread 0}
   ]
   :conversations {:chat {}, :groupchat {}}})

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

(defn logged-in-user
  [app-state]
  (:user app-state))

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
    (update-in app-state [:conversations type from] (fnil conj []) chat)))

(defn add-own-chat
  [app-state chat]
  (update-in app-state [:conversations :chat (:to chat)] (fnil conj []) chat))

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
  [app-state user]
  (get-in app-state [:presence (get-in user [:jid :full])]))

(defn room
  ;; SHOULD BE DEPRECATED in favor of get-room
  [app-state name]
  (first (filter #(= name (get-in % [:jid :local]))
                 (:channels app-state))))

(defn get-room
  [state name]
  (let [channels (:channels state)
        filtered (filter (fn [ch] (= name (get-in ch [:jid :local]))) channels)]
    (first filtered)))

(defn contact
  [app-state name]
  (first (filter #(= name (get-in % [:jid :local]))
                 (:roster app-state))))

(defn room-messages
  [app-state room]
  (get-in app-state [:conversations :groupchat (get-in room [:jid :bare])]))

(defn get-room-messages
  [state room]
  (let [roomaddress (get-in room [:jid :bare])]
    (get-in state [:conversations :groupchat roomaddress])))

(defn contact-messages
  [app-state user]
  (get-in app-state [:conversations :chat (get-in user [:jid :bare])]))
