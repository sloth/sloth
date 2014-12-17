(ns openslack.browser)

(defn allow-notifications?
  []
  (= (.-permission js/Notification) "granted"))

(defn scroll-to-bottom
  [query]
  (let [element (.querySelector js/document query)
        height (.-scrollHeight element)]
    (set! (.-scrollTop element) height)))

(defn notify
  [title body icon]
  (js/Notification. title #js {:body body :icon icon}))
