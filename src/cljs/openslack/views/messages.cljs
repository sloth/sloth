(ns openslack.views.messages
  (:require [om.core :as om :include-macros true]
            [sablono.core :as s :include-macros true]
            [openslack.browser :as browser]
            [openslack.state :as st]
            [openslack.text :refer [enrich-text]]))


(defn message
  [state->author]
  (fn
    [state owner]
    (reify
      om/IDisplayName
      (display-name [_] "Message")

      om/IDidMount
      (did-mount [this]
        (browser/scroll-to-bottom ".messages-container"))

      om/IRender
      (render [_]
        (let [author (state->author state)
              logged-in-user (st/logged-in-user @st/state)
              is-my-message? (= author (:local logged-in-user))
              classes (if is-my-message?
                        ["message" "self"]
                        ["message"])
              class-name (apply str (interpose " " classes))]
          (s/html
           [:div {:class-name class-name}
            [:div.message-avatar
             [:img
              {:height "35",
               :width "35",
               :alt "#user",
               :src (:avatar state)}]]
            [:div.message-content
             [:div.message-title [:strong author]
              [:span (let [stamp (:timestamp state)
                           hours (.getHours stamp)
                           mins (.getMinutes stamp)
                           mins (if (< mins 10)
                                  (str "0" mins)
                                  mins)]
                       (str hours ":" mins))]]
             [:p.content (enrich-text (:body state))]]]))))))

(def room-message (message #(get-in % [:from :resource])))
(def contact-message (message #(get-in % [:from :local])))
