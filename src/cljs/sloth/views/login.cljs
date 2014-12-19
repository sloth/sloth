(ns sloth.views.login
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :refer [<!]]
            [sloth.auth :as auth]
            [sloth.state :as st]
            [sloth.xmpp :as xmpp]
            [sloth.routing :as routing]
            [cats.core :as m :include-macros true]
            [cats.monad.either :as either]))

(defn do-login
  [owner {:keys [username password]}]
  (go
    (let [msession (<! (auth/authenticate username password))]
      (cond
       (either/right? msession)
       (routing/navigate "")

       (either/left? msession)
       (let [state (om/get-state owner)]
         (om/set-state! owner (assoc state :error "Wrong credentials!")))))))

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
                         :placeholder "Login"
                         :autocomplete "off"
                         :on-change (fn [ev]
                                      (om/set-state! owner (assoc state :username (.-value (.-target ev)))))
                         :default-value (:username state)}]
                 [:input {:type "password"
                          :placeholder "Password"
                          :on-change (fn [ev]
                                      (om/set-state! owner (assoc state :password (.-value (.-target ev)))))
                          :default-value (:password state)}]
                 [:button {:on-click (fn [ev]
                                       (.preventDefault ev)
                                       (do-login owner state))} "Login"]]]
               [:div.dat-sloth]
               [:div.dat-text "Open source team communication plataform"]
               [:div.dat-bubble-arrow]]]
             (when (:error state)
               [:p (:error state)])]))))
