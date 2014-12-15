(ns openslack.routing
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [openslack.events :as events]
            [openslack.state :as st]
            [secretary.core :as secretary :refer-macros [defroute]])
  (:import goog.History))

;; History

(def history (History.))

(defn navigate
  [token]
  (.setToken history token))


(defn start-history!
  []
  ;; Config routing
  (secretary/set-config! :prefix "#")
  ;; Listen for navigate
  (go
    (let [event (<! (events/listen history "navigate"))]
      (secretary/dispatch! (.-token event))))
  ;; Enable history
  (.setEnabled history true)
  ;; Dispacth initial route
  (secretary/dispatch! (.getToken history)))

;; Routes

(defroute home-route "/" []
  (if (:user @st/state)
    (swap! st/state assoc :page {:name :home})
    (navigate "/login")))

(defroute login-route "/login" []
  (if (:user @st/state)
    (navigate "")
    (swap! st/state assoc :page {:name :login})))

(defroute room-route "/room/:jid" [jid]
  (if (:user @st/state)
    (swap! st/state assoc :page {:name :room, :jid jid})
    (navigate "/login")))

(defroute contact-route "/contact/:jid" [jid]
  (if (:user @st/state)
    (swap! st/state assoc :page {:name :contact, :jid jid})
    (navigate "/login")))

(defroute catch-all-route "*" []
  ; FIXME: invalid route
  (if (:user @st/state)
    (navigate "")
    (navigate "/login")))
