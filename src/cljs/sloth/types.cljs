(ns sloth.types
  (:require [shodan.console :as console :include-macros true]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; JID
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn rjid->jid
  "Build chat identity instance from
  raw xmpp jid object from xmpp."
  [rjid]
  {:local (.-local rjid)
   :domain (.-domain rjid)
   :resource (.-resource rjid)
   :bare (.-bare rjid)
   :full (.-full rjid)
   :type ::jid})

(defn ->jid
  [local domain resource]
  {:local local
   :domain domain
   :resource resource
   :bare (str local "@" domain)
   :full (str local "@" domain "/" resource)
   :type ::jid})

;; Useful functions

(defmulti get-user-local
  "Get local representation of user/jid."
  :type)

(defmulti get-user-bare
  "Get bare representation of user/jid."
  :type)

(defmulti get-user-resource
  "Get resource representation of user/jid."
  :type)

;; Useful functions implementation

(defmethod get-user-local ::jid
  [jid]
  (:local jid))

(defmethod get-user-bare ::jid
  [jid]
  (:bare jid))

(defmethod get-user-resource ::jid
  [jid]
  (:resource jid))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Roster
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ->roster-item
  [{:keys [jid subscription groups]}]
  {:type ::rosteritem
   :jid jid
   :subscription subscription
   :groups groups})

(defmethod get-user-local ::rosteritem
  [rosteritem]
  (get-in rosteritem [:jid :local]))

(defmethod get-user-bare ::rosteritem
  [rosteritem]
  (get-in rosteritem [:jid :bare]))

;; TODO: not needed at this moment.
;; (defmethod get-user-resource ::rosteritem
;;   [rosteritem]
;;   (get-in rosteritem [:jid :resource]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Presence
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ->presence
  [{:keys [from to status availability priority]}]
  {:type ::presence
   :from from
   :to to
   :status status
   :availability availability
   :priority priority})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Chat
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn rchat->chat
  "Build chat instance from raw xmpp
  char instance."
  [rchat]
  (let [chattype (keyword (.-type rchat))]
    (-> {:from (rjid->jid (.-from rchat))
         :to (rjid->jid (.-to rchat))
         :id (.-id rchat)
         :body (.-body rchat)
         :type (case chattype
                 :chat ::chat
                 :groupchat :groupchat)}
        (merge (if-let [delay (.-delay rchat)]
                 {:timestamp (.-stamp delay)
                  :delay (js->clj delay)}
                 {:timestamp (js/Date.)})))))


(defn ->chat
  [{:keys [timestamp type id] :as msg}]
  (let [chattype (case type
                   :chat ::chat
                   :groupchat ::groupchat)]
    (-> (assoc msg :type chattype)
        (assoc :id (if (nil? id)
                     (str (gensym 'sloth))
                     id))
        (assoc :timestamp (if (nil? timestamp)
                            (js/Date.)
                            timestamp)))))


