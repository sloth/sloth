(ns openslack.xmpp
  (:require [cljs.core.async :as async]
            [cats.monad.either :as either]))

(defn create-client [config]
  (.createClient js/XMPP (clj->js config)))

(defn raw-jid->jid
  [rjid]
  {:bare (.-bare rjid)
   :local (.-local rjid)
   :domain (.-domain rjid)
   :resource (.-resource rjid)
   :full (.-full rjid)})

(defn start-session [client]
  (let [c (async/chan 1)]
    (.connect client)
    (.on client "session:started" (fn [rjid]
                                    (.sendPresence client)
                                    (->> (if (= (.-type rjid) "error")
                                           (either/left (keyword (.-condition (.-error rjid))))
                                           (either/right (raw-jid->jid rjid)))
                                         (async/put! c))))
    c))

(defn raw-roster->roster [rroster]
  (into [] (map (fn [ritem]
                  {:jid (raw-jid->jid (.-jid ritem))
                   :subscription (keyword (.-subscription ritem))})
                (.-items (.-roster rroster)))))

(defn get-roster [client]
  (let [c (async/chan 1)]
    (.getRoster client (fn [_ rroster]
                          (->> (if (= (.-type rroster) "error")
                                 (either/left (keyword (.-condition (.-error rroster))))
                                 (either/right (raw-roster->roster rroster)))
                                (async/put! c))))
    c))
