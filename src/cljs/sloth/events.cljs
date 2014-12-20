(ns sloth.events
  (:require [cljs.core.async :refer [<! put! chan]]
            [goog.events :as events]))

(defn listen
  ([target event] (listen target event (chan)))
  ([target event ch]
   (events/listen target event
                  (fn [event]
                    (let [r (put! ch event)]
                      (when (nil? r)
                        (events/unlisten target event)))))
   ch))

(defn pressed-enter?
  [event]
  (= (.-keyCode event) 13))
