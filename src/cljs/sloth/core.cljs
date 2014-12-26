(ns sloth.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :refer [<!]]
            [weasel.repl :as ws-repl]
            ;; [figwheel.client :as fw]
            [sloth.config :as config]
            [sloth.auth :as auth]
            [sloth.routing :refer [start-history!]]
            [sloth.state :as st]
            [sloth.xmpp :as xmpp]
            [sloth.views :as views]
            [sloth.browser :as browser]
            [hodgepodge.core :refer [local-storage]]
            [shodan.console :as console :include-macros true]
            [cats.core :as m]
            [cats.monad.either :as either]))

(enable-console-print!)

;; Enable browser enabled repl.
;;(ws-repl/connect "ws://localhost:9001")

(defn- initialize-roster
  [client]
  (go-loop []
    (let [mroster (<! (xmpp/get-roster client))]
      (if (either/right? mroster)
        (st/update-roster (either/from-either mroster))
        (recur)))))

(defn- initialize-bookmarks
  [client]
  (go []
    (let [bookmarks (<! (xmpp/get-bookmarks client))]
      (swap! st/state assoc :bookmarks bookmarks)
    (doseq [room (:conferences bookmarks)]
      (let [room (<! (xmpp/join-room client (:bare room) (:local (:user @st/state))))]
        (st/add-room room))))))

(defn- start-presence-watcher
  [client]
  (let [presence-chan (xmpp/presences client)]
    (go-loop [presence (<! presence-chan)]
      (swap! st/state st/update-presence presence)
      (recur (<! presence-chan)))))

(defn- start-chat-watcher
  [client]
  (let [chats (xmpp/chats client)]
    (go-loop []
      (when-let [message (<! chats)]
        (browser/notify-if-applies message)
        (condp = (:type message)
          :chat (st/insert-private-message (:from message) message)
          :groupchat (st/insert-group-message message))
        (recur)))))

(defn- start-muc-watcher
  [client]
  (let [subjects (xmpp/subjects client)
        invitations (xmpp/room-invitations client)]

    (go-loop []
      (when-let [subject (<! subjects)]
        (st/set-room-subject (:room subject) (:subject subject))
        (recur)))

    (go-loop []
      (when-let [invitation (<! invitations)]
        (st/add-room-invitation invitation)
        (recur)))))

(defn- start-focus-watcher
  []
  (let [events (browser/listen-focus-events)]
    (go-loop []
      (when-let [event (<! events)]
        (swap! st/state assoc :window-focus event)
        (recur)))))

(defn- start-raw-packets-watcher
  [client]
  ;; Raw events (debug)
  (.on client "raw:*" (fn [ev payload]
                               (.log js/console "******************************")
                               (.log js/console ev))))

(def bootstrap (atom false))

(defn bootstrap-session
  []
  (go
    (when-let [authdata (:auth @st/state)]
      ;; TODO: handle error
      (let [res (<! (auth/authenticate (:username authdata)
                                       (:password authdata)))]
        (cond
         (either/left? res)
         (do
           (println "ERROR ON REAUTH")))))

    (let [state @st/state
          client (:client state)]

      ;; Only for debug
      ;; (start-raw-packets-watcher client)
      ;; Send initial
      (xmpp/send-presence client (get state :user-presence {}))

      ;; Start watchers
      (initialize-roster client)
      (initialize-bookmarks client)
      (start-presence-watcher client)
      (start-chat-watcher client)
      (start-muc-watcher client)
      (start-focus-watcher)
      )))

(defn mount-root-component
  []
  (om/root views/app st/state
           {:target (js/document.querySelector "body")}))

(defn start-login-watcher
  "Watch login changes and initialize xmpp session."
  []
  (add-watch st/state :log-in
             (fn [_ _ oldval newval]
               (when (and (st/logged-in? newval)
                          (not @bootstrap))
                 (bootstrap-session)
                 (.requestPermission js/Notification)
                 (swap! bootstrap not)))))

(defn start-state-persistence
  []
  (add-watch st/state :persistence
             (fn [_ _ oldval newval]
               ;; TODO: use select-keys here?
               (let [state (dissoc newval
                                   :client :user :roster :presence :chats :groupchats
                                   :features :page :conversations :window-focus
                                   :bookmarks :rooms)]
                 (assoc! local-storage :state state)))))

(defn main
  []
  ;; Start processes
  (start-login-watcher)
  (start-state-persistence)

  ;; Restore previously stored state
  (if-let [storedstate (:state local-storage nil)]
    (st/set-initial-state storedstate)
    (st/set-initial-state))

  ;; Start routing
  (start-history!)

  ;; Mount main om component
  (mount-root-component))

(main)
