(ns sloth.views.login
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [shodan.console :as console :include-macros true]
            [cuerdas.core :as str]
            [cats.monad.either :as either]
            [cljs.core.async :refer [<!]]
            [sloth.auth :as auth]
            [sloth.state :as st]
            [sloth.xmpp :as xmpp]
            [sloth.routing :as routing]
            [sloth.events :as events]
            [sloth.react :as react]))

(defn try-login
  [owner {:keys [username password] :as form}]
  (go
    (console/log (pr-str form))
    (let [msession (<! (auth/authenticate username password))]
      (console/log (pr-str msession))
      (cond
        (either/right? msession)
        (routing/navigate "")

        (either/left? msession)
        (swap! form assoc :error "Wrong credentials!")))))

(defn- on-enter
  [{:keys [username password] :as state} owner]
  (console/log "on-enter"))
  ;; ;; TODO: valid username?
  ;; (when (every? (complement str/empty?) [username password])
  ;;   (try-login owner state)))

(defn- onkeyup
  [state owner event]
  (console/log "onkeyup"))
  ;; (when (events/pressed-enter? event)
  ;;   (on-enter state owner)))

;; (defn- init-state
;;   [owner props]
;;   (assoc owner :form (atom {})))

(def form (atom {}))

(defn- render-state
  [{:keys [form] :as owner} meta-state app-state]
  (html
   [:div.lightbox-shadow
    [:div.lightbox
     [:div.login
      [:div.login-form
       [:div.logo]
       [:span (:username @form)]
       [:form
        [:input {:type "text"
                 :value (:username @form)
                 :placeholder "you@sloth.land"
                 :auto-complete "off"
                 :autofocus true
                 :on-change (fn [ev]
                              (console/log "on-change" (pr-str form))
                              (swap! form assoc :username (.-value (.-target ev))))
                 :default-value (:username @form)}]
        [:input {:type "password"
                 :placeholder "I ♥ sloths"
                 :on-change (fn [ev] (swap! form assoc :password (.-value (.-target ev))))
                 :default-value (:password @form)
                 :on-key-up (partial onkeyup owner nil)}]
        [:button {:on-click (fn [ev]
                              (.preventDefault ev)
                              (try-login owner @form))} "Login"]]]
      [:div.dat-sloth]
      [:div.dat-text "Open source team communication plataform"]
      [:div.dat-bubble-arrow]]]
    (when (:error @form)
      [:p (:error @form)])]))

(defn- will-mount
  [{:keys [form] :as owner}]
  (console/log (pr-str owner))
  (let [comp (react/get-component owner)]
    (add-watch form :form
               (fn [_ _ _ v]
                 (console/log "watch" (pr-str v))
                 (react/request-render comp)))
    owner))

(defn- will-unmount
  [{:keys [form] :as owner}]
  (remove-watch form :form))

(defn- init-state
  [owner]
  (assoc owner :form (atom {})))

(def login-component
  (react/component
   {:init init-state
    :will-mount will-mount
    :will-unmount will-unmount
    :render-state render-state}))
