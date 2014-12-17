(ns openslack.browser)

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
                   1000)))


(defn notify
  [title body icon]
  (js/Notification. title #js {:body body :icon icon}))
