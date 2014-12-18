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
      {:username "kim@niwi.be"
       :password "korea"
       :error ""})

    om/IRenderState
    (render-state [_ state]
      (html [:form
              [:label "Username"]
              [:input {:type "text"
                       :placeholder "you@server.com"
                       :on-change (fn [ev]
                                    (om/set-state! owner (assoc state :username (.-value (.-target ev)))))
                       :default-value (:username state)}]
              [:label "Password"]
              [:input {:type "password"
                       :on-change (fn [ev]
                                    (om/set-state! owner (assoc state :password (.-value (.-target ev)))))
                       :default-value (:password state)}]
              [:input {:type "button"
                       :value "Log in"
                       :on-click (fn [ev]
                                   (.preventDefault ev)
                                   (do-login owner state))}]
             (when (:error state)
               [:p (:error state)])]))))
