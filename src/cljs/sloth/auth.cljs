(ns sloth.auth
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [shodan.console :as console :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :refer [<!]]
            [sloth.state :as st]
            [sloth.xmpp :as xmpp]
            [sloth.routing :as routing]
            [cats.core :as m :include-macros true]
            [cats.monad.either :as either]))

(defn authenticate
  [username password]
  (go
    (let [msession (<! (xmpp/authenticate username password))]
      (console/log "auth$end" (pr-str msession))
      (m/>>= msession
             (fn [session]
               (st/initialize-session session)
               (m/return session))))))
