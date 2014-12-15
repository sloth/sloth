(ns openslack.routing
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [openslack.events :as events]
            [openslack.state :as st]
            [secretary.core :as secretary :refer-macros [defroute]])
  (:import goog.History))

;; Routes

(defroute home-route "/" []
  (swap! st/state assoc :page {:name :home}))

(defroute login-route "/login" []
  (swap! st/state assoc :page {:name :login}))

(defroute room-route "/room/:jid" [jid]
  ; TODO: validate jid
  (swap! st/state assoc :page {:name :room
                                  :jid jid}))

(defroute contact-route "/contact/:jid" [jid]
  ; TODO: validate jid
  (swap! st/state assoc :page {:name :contact
                                  :jid jid}))

;; Start history

(defn navigate
  [route]
  (secretary/dispatch! route))

(defn start-history!
  []
  (let [history (History.)]
    ;; Config routing
    (secretary/set-config! :prefix "#")
    ;; Listen for navigate
    (go
      (let [event (<! (events/listen history "navigate"))]
        (secretary/dispatch! (.-token event))))
    ;; Enable history
    (.setEnabled history true)
    ;; Dispacth initial route
    (secretary/dispatch! (.getToken history))))
