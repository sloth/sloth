(ns openslack.state
  (:require [shodan.console :as console :include-macros true]
            [openslack.browser :as browser]))


; TODO: schema of state?
(defn make-initial-state []
  {:page {:state :login}
   :client nil
   :user nil
   :features []
   :roster {}
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
   :channels {:sloth {:local "sloth"
                      :bare "sloth@conference.niwi.be"
                      :unread 0}
              :testroom {:local "testroom"
                         :bare "testroom@conference.niwi.be"
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

(defn add-chat
  [app-state chat]
  (let [type (:type chat)
        from (get-in chat [:from :bare])]
    (update-in app-state [ type from] (fnil conj []) chat)))

(defn- insert-group-message
  [from message]
  (let [recipient (get from :bare)]

    (browser/notify-if-applies message)
    (swap! state (fn [state]
                   (update-in state [:groupchats recipient] (fnil conj []) message)))))

(defn- insert-private-message
  [from message]
  (let [recipient (get from :bare)]
    (swap! state (fn [state]
                   (update-in state [:chats recipient] (fnil conj []) message)))))

(defn insert-message
  [from message]
  (condp = (:type message)
    :chat (insert-private-message from message)
    :groupchat (insert-group-message from message)))

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
  [state name]
  (let [channels (:channels state)
        name (keyword name)]
    (get channels name)))

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
