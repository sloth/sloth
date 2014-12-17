(ns openslack.views.messages
  (:require [om.core :as om :include-macros true]
            [sablono.core :as s :include-macros true]))

(defn message-input
  [send-message-fn]
  (fn [_ owner]
    (reify
      om/IDisplayName
      (display-name [_] "Message Input")

      om/IInitState
      (init-state [_]
        {:message ""})

      om/IRenderState
      (render-state [_ {:keys [message]}]
        (s/html
         [:div.write-message
          [:textarea
           {:value message
            :auto-focus true
            :on-change (fn [e] (om/set-state! owner :message (.-value (.-target e))))
            :on-key-down (fn [e]
                           (when (= (.-keyCode e) 13)
                             (if (or (.-ctrlKey e) (.-shiftKey e))
                               (om/set-state! owner :message (str message "\n"))
                               (do
                                 (.preventDefault e)
                                 (send-message-fn message)
                                 (om/set-state! owner :message "")))))}]])))))

(defn contact-message
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
         [:div.message-title [:strong (get-in state [:from :local])]
                             [:span (let [stamp (:timestamp state)
                                          hours (.getHours stamp)
                                          mins (.getMinutes stamp)
                                          mins (if (< mins 10)
                                                 (str "0" mins)
                                                 mins)]
                                      (str hours ":" mins))]]
         [:p.content (:body state)]]]))))

(defn room-message
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
         [:div.message-title [:strong (get-in state [:from :resource])]
                             [:span (let [stamp (:timestamp state)
                                          hours (.getHours stamp)
                                          mins (.getMinutes stamp)
                                          mins (if (< mins 10)
                                                 (str "0" mins)
                                                 mins)]
                                      (str hours ":" mins))]]
         [:p.content (:body state)]]]))))
