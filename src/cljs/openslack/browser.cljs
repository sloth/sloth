(ns openslack.browser
  (:require [openslack.state :as st]
            [shodan.console :as console :include-macros true]
            [cuerdas.core :as str]))

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
    (.setTimeout js/window #(.close notification) 3000)))

(defn notify-if-applies
  [message]
  (let [username (str/trim (get-in @st/state [:user :local]))
        author (str/trim (get-in message [:from :resource]))
        channel-name (get-in message [:from :local])
        title (str author "@" channel-name)
        body (:body message)]

    (if (and (allow-notifications?) (not= username author))
      (notify title body))))
