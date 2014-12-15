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
  (.setToken history (.replace token #"^#" ""))
  (secretary/dispatch! (.getToken history)))

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
  (if (st/logged-in?)
    (swap! st/state assoc :page {:state :home})
    (navigate "/login")))

(defroute login-route "/login" []
  (if (st/logged-in?)
    (navigate "")
    (swap! st/state assoc :page {:state :login})))

(defroute room-route "/room/:name" [name]
  (.log js/console "ROOOM " name)
  (if (st/logged-in?)
    (swap! st/state assoc :page {:state :room, :room name})
    (navigate "/login")))

(defroute contact-route "/contact/:jid" [jid]
  (if (st/logged-in?)
    (swap! st/state assoc :page {:state :contact, :jid jid})
    (navigate "/login")))

(defroute catch-all-route "*" []
  ; FIXME: invalid route
  (if (st/logged-in?)
    (navigate "")
    (navigate "/login")))
