(ns sloth.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :refer [<!]]
            ;; [weasel.repl :as ws-repl]
            ;; [figwheel.client :as fw]
            ;; [hodgepodge.core :refer [local-storage]]
            [hodgepodge.core :as hodgepodge]
            [shodan.console :as console :include-macros true]
            [cats.core :as m]
            [cats.monad.either :as either]
            [sloth.config :as config]
            [sloth.auth :as auth]
            [sloth.routing :refer [start-history!]]
            [sloth.state :as st]
            [sloth.xmpp :as xmpp]
            [sloth.views :as views]
            [sloth.browser :as browser]))

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
      (swap! st/app-state assoc :bookmarks bookmarks)
    (doseq [room (:conferences bookmarks)]
      (let [logged-user (st/get-logged-user)
            room (<! (xmpp/join-room client
                                     (:bare room)
                                     (:local logged-user)))]
        (st/add-room room))))))

(defn- start-presence-watcher
  [client]
  (let [presence-chan (xmpp/presences client)]
    (go-loop [presence (<! presence-chan)]
      (swap! st/app-state st/update-presence presence)
      (recur (<! presence-chan)))))

(defn- start-chat-watcher
  [client]
  (let [chats (xmpp/chats client)]
    (go-loop []
      (when-let [message (<! chats)]
        (browser/notify-if-applies message)

        (condp = (:type message)
          :sloth.types/chat (st/insert-private-message (:from message) message)
          :sloth.types/groupchat (st/insert-group-message message)
          (console/error "start-chat-watcher: no matching message type"))
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
        (swap! st/app-state assoc :window-focus event)
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
    (when-let [authdata (:auth @st/app-state)]
      (let [res (<! (auth/authenticate (:username authdata)
                                       (:password authdata)))]
        (cond
         (either/left? res)
         (do
           (println "ERROR ON REAUTH")))))

    (let [state @st/app-state
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
  (om/root views/app st/app-state
           {:target (js/document.querySelector "body")}))

(defn start-login-watcher
  "Watch login changes and initialize xmpp session."
  []
  (letfn [(watcher [_ _ _ state]
            (when (and (st/logged-in? state)
                       (not @bootstrap))
              (bootstrap-session)
              (.requestPermission js/Notification)
              (swap! bootstrap not)))]
    (add-watch st/meta-state :auth watcher)))

(defn start-state-persistence
  "Watch state changes and persist them
  into local storage."
  []
  (letfn [(persist [state]
            (let [state (select-keys state [:auth])]
              (assoc! hodgepodge/local-storage :state state)))]
    (add-watch st/app-state :persistence #(persist %4))))

(defn main
  []
  ;; Start processes
  (start-login-watcher)
  (start-state-persistence)

  ;; Restore previously stored state
  (if-let [storedstate (:state hodgepodge/local-storage nil)]
    (st/initialize-state storedstate)
    (st/initialize-state))

  ;; Start routing
  (start-history!)

  ;; Mount main om component
  (mount-root-component))

(main)
