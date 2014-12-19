(ns sloth.routing
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [sloth.events :as events]
            [sloth.state :as st]
            [secretary.core :as secretary :refer-macros [defroute]])
  (:import [goog History]
           [goog.history EventType]))

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

  ;; Listen for navigate and hash changes
  (let [event-chan (events/listen history EventType.NAVIGATE)]
    (go
      (let [event (<! event-chan)]
        (secretary/dispatch! (.-token event)))))

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
  (if (st/logged-in?)
    (swap! st/state
           (fn [state]
             (-> state
                 (assoc :page {:state :room :room name})
                 (update-in [:channels (keyword name)] assoc :unread 0))))
    (navigate "/login")))

(defroute contact-route "/contact/:name" [name]
  (if (st/logged-in?)
    (swap! st/state assoc :page {:state :contact, :contact name})
    (navigate "/login")))

(defroute catch-all-route "*" []
  ; FIXME: invalid route
  (if (st/logged-in?)
    (navigate "")
    (navigate "/login")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Misc URL factories
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn emoji-route
  [emoji]
  (str  "static/imgs/emoji/" emoji ".png"))

(def placeholder-avatar-route "static/imgs/placeholder-avatar.png")

(defn static-image-route
  [image]
  (str "static/imgs/" image))
