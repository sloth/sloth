(ns sloth.views.login
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cuerdas.core :as str]
            [cljs.core.async :refer [<!]]
            [sloth.auth :as auth]
            [sloth.state :as st]
            [sloth.xmpp :as xmpp]
            [sloth.routing :as routing]
            [sloth.events :as events]
            [cats.monad.either :as either]))

(defn try-login
  [owner {:keys [username password]}]
  (go
    (let [msession (<! (auth/authenticate username password))]
      (cond
       (either/right? msession)
       (routing/navigate "")

       (either/left? msession)
       (let [state (om/get-state owner)]
         (om/set-state! owner (assoc state :error "Wrong credentials!")))))))

(defn- on-enter
  [{:keys [username password] :as state} owner]
  ;; TODO: valid username?
  (when (every? (complement str/empty?) [username password])
    (try-login owner state)))

(defn- onkeyup
  [state owner event]
  (when (events/pressed-enter? event)
    (on-enter state owner)))

(defn login
  [_ owner]
  (reify
    om/IDisplayName
    (display-name [_] "Login")

    om/IInitState
    (init-state [_]
      {:username ""
       :password ""
       :error ""})

    om/IRenderState
    (render-state [_ state]
      (html [:div.lightbox-shadow
             [:div.lightbox
              [:div.login
               [:div.login-form
                [:div.logo]
                [:form
                 [:input {:type "text"
                          :placeholder "you@sloth.land"
                          :autocomplete "off"
                          :autofocus true
                          :on-change (fn [ev]
                                       (om/set-state! owner (assoc state :username (.-value (.-target ev)))))
                          :default-value (:username state)}]
                 [:input {:type "password"
                          :placeholder "I ♥ sloths"
                          :on-change (fn [ev]
                                      (om/set-state! owner (assoc state :password (.-value (.-target ev)))))
                          :default-value (:password state)
                          :on-key-up (partial onkeyup state owner)}]
                 [:button {:on-click (fn [ev]
                                       (.preventDefault ev)
                                       (try-login owner state))} "Login"]]]
               [:div.dat-sloth]
               [:div.dat-text "Open source team communication plataform"]
               [:div.dat-bubble-arrow]]]
             (when (:error state)
               [:p (:error state)])]))))
