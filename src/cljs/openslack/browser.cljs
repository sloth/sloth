(ns openslack.browser
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [openslack.state :as st]
            [shodan.console :as console :include-macros true]
            [cuerdas.core :as str]
            [cljs.core.async :as async :refer [put! chan]]))

(defn allow-notifications?
  []
  (= (.-permission js/Notification) "granted"))

(defn- calculate-max-scroll
  [element]
  (let [scroll-height (.-scrollHeight element)
        outer-height (.-offsetHeight element)]
    (- scroll-height outer-height)))

(defn scroll-to-bottom
  [query]
  (let [element (.querySelector js/document query)
        maxscroll (calculate-max-scroll element)]
    (set! (.-scrollTop element) maxscroll)
    (js/setTimeout (fn []
                     (let [maxscroll (calculate-max-scroll element)]
                       (set! (.-scrollTop element) maxscroll)))
                   500)))

(defn notify
  [title body]
  (let [icon "/static/imgs/placerholder-avatar-1.jpg"
        notification (js/Notification. title #js {:body body :icon icon})]
    (js/setTimeout #(.close notification) 3000)))

(defn play-notification-sound
  []
  (let [audio (.querySelector js/document "#notification-sound")]
    (.play audio)))

(defn notify-if-applies
  [message]
  (go
    (let [username (str/trim (get-in @st/state [:user :local]))
          author (str/trim (get-in message [:from :resource]))
          channel-name (get-in message [:from :local])
          title (str author "@" channel-name)
          body (:body message)]

      (when (and (allow-notifications?) (not= username author) (not (st/window-focused?)))
        (play-notification-sound)
        (notify title body)))))


(defn listen-focus-events
  ([]
   (listen-focus-events (chan)))
  ([channel]
   (set! (.-onfocus js/window) (fn [e] (put! channel :focus)))
   (set! (.-onblur js/window) (fn [e] (put! channel :blur)))
   channel))
