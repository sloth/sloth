(ns sloth.routing
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [sloth.events :as events]
            [sloth.state :as st :refer [app-state]]
            [secretary.core :as secretary :refer-macros [defroute]])
  (:import [goog History]
           [goog.history EventType]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; HTML History
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

(defn set-route
  "Set route in the current app state."
  ([name] (set-route name nil))
  ([name params]
   (swap! app-state st/set-route name params)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Routes
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defroute home-route "/" []
  (if (st/logged-in?)
    (set-route :home)
    (navigate "/login")))

(defroute login-route "/login" []
  (if (st/logged-in?)
    (navigate "")
    (set-route :login)))

(defroute room-route "/room/:name" [name]
  (if (st/logged-in?)
    (do
      (set-route :room {:room name})
      ;; TODO: abstract with function instead of direct state manipulation
      (swap! st/app-state #(update-in % [:rooms (keyword name)] assoc :unread 0)))
    (navigate "/login")))

(defroute contact-route "/contact/:name" [name]
  (if (st/logged-in?)
    (set-route :contact {:contact name})
    (navigate "/login")))

(defroute catch-all-route "*" []
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
