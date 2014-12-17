(ns openslack.auth
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :refer [<!]]
            [openslack.state :as st]
            [openslack.xmpp :as xmpp]
            [openslack.routing :as routing]
            [cats.core :as m :include-macros true]
            [cats.monad.either :as either]))

(defn authenticate
  [username password]
  (go
    (let [msession (<! (xmpp/authenticate username password))]
      (m/>>= msession
             (fn [session]
               (st/initialize-session session)
               (m/return session))))))
