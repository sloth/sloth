(ns openslack.views.message
  (:require [om.core :as om :include-macros true]
            [sablono.core :as s :include-macros true]))
            ;; [openslack.state :as st]))

(defn message
  [state owner]
  (reify
    om/IDisplayName
    (display-name [_] "Message")

    om/IRender
    (render [_]
      (s/html
       [:div.message
        [:div.message-avatar
         [:img
          {:height "35",
           :width "35",
           :alt "#user",
           :src (:avatar state)}]]
        [:div.message-content
         [:div.message-title [:strong (-> state :from :resource)] [:span "10:11 pm"]]
         [:p.content (-> state :body)]]]))))
