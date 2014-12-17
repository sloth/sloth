(ns openslack.browser)

(defn allow-notifications?
  []
  (= (.-permission js/Notification) "granted"))

(defn scroll-to-bottom
  [query]
  (let [element (.querySelector js/document query)
        height (.-offsetHeight element)]
    (set! (.-scrollTop element) height)))

(defn notify
  [title body icon]
  (js/Notification. title (js-obj :body body :icon icon)))
