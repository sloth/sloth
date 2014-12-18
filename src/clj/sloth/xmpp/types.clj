(ns sloth.xmpp.types)

(defrecord Jid [local domain resource])
(defrecord Message [type body subject from to])
(defrecord Presence [type from to status priority show])
(defrecord Unknown [data])
