(ns openslack.xmpp.types)

(defrecord Jid [local domain resource])
(defrecord Message [type body subject from to])
(defrecord Unknown [data])
