(ns sloth.views.login
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [shodan.console :as console :include-macros true]
            [cuerdas.core :as str]
            [cljs.core.async :refer [<!]]
            [sloth.auth :as auth]
            [sloth.state :as st]
            [sloth.xmpp :as xmpp]
            [sloth.routing :as routing]
            [sloth.events :as events]
            [cats.monad.either :as either]))

(defn login
  [owner {:keys [username password] :as local}]
  (go
    (console/log "login" (pr-str local))
    (let [msession (<! (auth/authenticate username password))]
      (cond
       (either/right? msession)
       (routing/navigate "/")

       (either/left? msession)
       (om/set-state! owner :error "Wrong credentials!")))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Events
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- on-submit
  [event owner local]
  ;; (.preventDefault event)
  (login owner local))

(defn- on-username-changed
  [event owner local]
  (let [value (.-value (.-target event))]
    (om/set-state! owner (assoc local :username value))))

(defn- on-password-changed
  [event owner local]
  (let [value (.-value (.-target event))]
    (om/set-state! owner (assoc local :password value))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Component
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- render-state
  [state owner {:keys [username password] :as local}]
  (html
   [:div.lightbox-shadow
    [:div.lightbox
     [:div.login
      [:div.login-form
       [:div.logo]
       [:form {:on-submit #(on-submit % owner local)}
        [:input {:type "text"
                 :placeholder "you@sloth.land"
                 :auto-complete "off"
                 :value username
                 :autofocus true
                 :on-change #(on-username-changed % owner local)}]
        [:input {:type "password"
                 :value password
                 :placeholder "I ♥ sloths"
                 :on-change #(on-password-changed % owner local)}]
        [:input {:type "submit"
                 :value "Login"}]]]
      [:div.dat-sloth]
      [:div.dat-text "Open source team communication plataform"]
      [:div.dat-bubble-arrow]]
     (when (:error state)
       [:p (:error state)])]]))

(defn login-component
  [state owner]
  (reify
    om/IDisplayName
    (display-name [_] "login")

    om/IInitState
    (init-state [_]
      {:username ""
       :password ""
       :error nil})

    om/IRenderState
    (render-state [_ local]
      (render-state state owner local))))
