(ns sloth.state
  (:require [om.core :as om :include-macros true]
            [cuerdas.core :as str]
            [sloth.types :as types]
            [shodan.console :as console :include-macros true]))

(defonce app-state (atom nil))
(defonce meta-state (atom nil)

(defn- initial-state
  "Build initial structure for the
  application state."
  []
  {:user-presence {}
   :features []
   :roster {}
   :presence {}
   :subscriptions []
   :window-focus :focus
   :rooms {}
   :room-invitations []
   :bookmarks {:conferences []}
   :chats {}
   :groupchats {}})

(defn- initial-meta-state
  "Build initial structure for the global
  meta state.

  Global meta state is mainly used for
  store auth and routing information."
  []
  {:page {:name :login}
   :client nil
   :user nil})

(defn initialize-state
  "Setup initial application and meta
  state."
  ([]
   (reset! meta-state (initial-meta-state))
   (reset! state (initial-state)))
  ([s]
   (reset! meta-state (initial-meta-state))
   (let [initial (initial-state)]
     (reset! state (merge initial s)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Routings
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn set-route
  "Set the current page with additional paramters."
  ([name] (set-route name nil))
  ([name params]
   (let [pagestate (merge {:name name} params)]
     (swap! meta-state assoc :page pagestate))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; User and Auth
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn logged-in?
  "Check if currently we are in
  loggedin state or not."
  ([] (logged-in? @meta-state))
  ([state]
   (not (nil? (:user state)))))

(defn get-logged-user
  "Get current logged user."
  []
  (:user @meta-state))

(defn get-client
  "Get current xmpp client instance."
  []
  (:client @meta-state))

(defn initialize-session
  "Store the current session in
  corresponding state."
  [{:keys [user client auth]}]
  (swap! meta-state assoc :client client :user user)
  (swap! state assoc :auth auth))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Rooms
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn insert-room
  "Insert room into joined room
  list on app state."
  [room]
  (let [roomid (keyword (get-in room [:jid :local]))]
    (swap! app-state assoc-in [:rooms roomid] room)))

(defn get-room
  ([name] (get-room @app-state name))
  ([state name]
   (let [rooms (:rooms state)
         roomkey (keyword name)]
     (get rooms roomkey))))

(defn get-current-room
  ([] (get-current-room @app-state))
  ([state]
   (when-let [roomname (get-in state [:page :room])]
     (get-room state roomname))))

(defn set-room-unread-messages
  [room n]
  (let [roomkey (keyword (:local room))]
    (swap! state
           (fn [state]
             (assoc-in state [:rooms roomkey :unread] n)))))

(defn clear-room-unread-messages
  [room]
  (set-room-unread-messages room 0))

(defn set-room-subject
  [roomjid subject]
  (let [roomkey (keyword (:local roomjid))]
    (swap! state (fn [st]
                   (assoc-in st [:rooms roomkey :subject] subject)))))

(defn add-room-invitation
  [invitation]
  (swap! state (fn [st]
                 (update-in st [:room-invitations] conj invitation))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Rooms
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


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
                       (update-in st [:rooms roomkey :unread] inc)))))

    (if (contains? message :subject)
      ;; Modify room subject
      (set-room-subject room (:subject message))
      ;; Insert message to state
      (swap! state (fn [st]
                     (update-in st [:groupchats recipient] (fnil conj []) message))))))

(defn insert-private-message
  [to message]
  (let [recipient (types/get-user-bare to)]
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
  "Get presence for any other person
  that is not self."
  ([user] (get-others-presence @app-state user))
  ([state user]
   (let [address (types/get-user-bare user)
         resources (get-in state [:presence address])
         presences (vals resources)]
     (first (sort-by :priority > presences)))))

(defn get-presence
  "Get presence information for user."
  ([user] (get-presence @app-state user))
  ([state user]
   ;; TODO: remove when
   (when user
     ;; (console/trace)
     (let [address1 (types/get-user-bare user)
           address2 (types/get-user-bare (:user state))]
       (if (= address1 address2)
         (:user-presence state)
         (get-others-presence state user))))))

(defn update-roster
  [roster]
  (let [roster (->> roster
                    (map (fn [o] [(keyword (types/get-user-local o)) o]))
                    (into {}))]
    (swap! state assoc :roster roster)))

(defn get-contact
  "Get roster entry by nickname."
  ([nickname] (get-contact @app-state nickname))
  ([state nickname]
   (let [contacts (:roster state)]
     (get contacts (keyword nickname)))))

(defn get-current-contact
  "Get roster entry by nickname."
  ([] (get-contact @app-state))
  ([state]
   (->> (get-in state [:page :contact])
        (get-contact state))))

(defn get-room-messages
  "Get room messages."
  [state room]
  (let [roomaddress (get-in room [:jid :bare])]
    (get-in state [:groupchats roomaddress])))

(defn get-room-message-entries
  "Get room messages grouped by recipient."
  [state room]
  (let [transform (comp
                   (partition-by (fn [msg] (get-in msg [:from :resource])))
                   (map (fn [entries]
                          {:messages entries
                           :from (:from (first entries))})))]
    (sequence transform (get-room-messages state room))))

(defn get-contact-messages
  "Get contact messages."
  [state user]
  (let [useraddress (types/get-user-bare user)]
    (get-in state [:chats useraddress])))

(defn get-contact-message-entries
  "Get contact messages grouped by recipient."
  [state user]
  (let [transformer (comp
                     ;; TODO: use future declared helper multimethod
                     (partition-by (fn [msg] (types/get-user-bare (:from msg))))
                     (map (fn [entries]
                            {:messages entries
                             :from (:from (first entries))})))]
    (sequence transformer (get-contact-messages state user))))

(defn window-focused?
  []
  (= :focus (:window-focus @app-state)))
