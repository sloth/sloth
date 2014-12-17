(ns openslack.browser)

(defn allow-notifications?
  []
  (= (.-permission js/Notification) "granted"))
