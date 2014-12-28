(ns sloth.browser
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [sloth.state :as st]
            [sloth.types :as types]
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

(defn- notify
  "Send browser notification."
  [{:keys [message author title]}]
  (let [user (st/get-logged-user)
        nickname (types/get-user-local user)
        body (:body message)]
    (when (and (allow-notifications?)
               (not= nickname author)
               (not (st/window-focused?)))

      ;; Play sound notification
      (let [audio (.querySelector js/document "#notification-sound")]
        (.play audio))

      ;; Show browser notification popup
      (let [icon "static/imgs/placerholder-avatar-1.jpg"
            options (clj->js {:body body :icon icon})
            notification (js/Notification. title options)]
        (js/setTimeout #(.close notification) 3000)))))

(defn notify-if-applies
  "Send browser notification and sound notification
  if message matches specific conditions."
  [message]
  (let [type (:type message)
        channel-name (get-in message [:from :local])]
    (condp = type
      :sloth.types/chat
      (let [author (str/trim (get-in message [:from :local]))]
        (notify {:message message
                 :author author
                 :title author}))

      :sloth.types/groupchat
      (let [author (str/trim (get-in message [:from :resource]))
            channel-name (get-in message [:from :local])
            title (str author "@" channel-name)]
        (notify {:message message
                 :author author
                 :title title}))

      (console/error "notify-if-applies: no matching message type" (pr-str type)))))


(defn listen-focus-events
  ([]
   (listen-focus-events (chan)))
  ([channel]
   (set! (.-onfocus js/window) (fn [e] (put! channel :focus)))
   (set! (.-onblur js/window) (fn [e] (put! channel :blur)))
   channel))
