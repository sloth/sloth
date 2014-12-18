(ns sloth.config
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn get-xmpp-config
  []
  (go
     {:transport :bosh
      :url "http://niwi.be:5280/http-bind"}))
