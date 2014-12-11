(ns openslack.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :refer [<!]]
            [secretary.core :as secretary :refer-macros [defroute]]
            [weasel.repl :as ws-repl]
            [openslack.utils :as utils])
  (:import goog.History))

;; Enable println
(enable-console-print!)

;; Enable browser enabled repl.
;; (ws-repl/connect "ws://localhost:9001")

(defn main
  []
  (let [history (History.)]
    (go
      (let [event (<! (utils/listen history "navigate"))]
        (secretary/dispatch! (.-token event))))
    (.setEnabled history true)))

(main)
