(ns openslack.state)

; TODO: schema of state?
(defn make-initial-state []
  {:page {:state :login}
   :client nil
   :features []
   :roster []
   :user nil
   :subscriptions [
    {:type :room
     :from {:jid "niwi@niwi.be"
            :local "niwi"}
     :room {:name "clojure"}}
    {:type :room
     :from {:jid "miguel@niwi.be"
            :local "miguel"}
     :room {:name "emacs"}}
    {:type :room
     :from {:jid "ramiro@niwi.be"
            :local "ramiro"}
     :room {:name "anime"}}
   ]
   :channels [
              {:name "sloth"
               :unread 3}
              {:name "general"
               :unread 2}
              {:name "random"
               :unread 0}
              ]
   :conversations {:chat {}, :groupchat {}}})

(def state (atom (make-initial-state)))

(defn logged-in?
  []
  (not (nil? (:user @state))))

(defn initialize-session
  [{:keys [user client]}]
  (swap! state assoc :client client)
  (swap! state assoc :user user))

(defn add-chat
  [app-state chat]
  (let [type (:type chat)
        from (-> chat :from :local)]
    (update-in app-state [:conversations type from] (fnil conj []) chat)))

(defn join-room
  [app-state room]
  (let [type :groupchat
        from (-> room :from :bare)]
    (update-in app-state [:conversations type from] (fnil empty []))))

(defn room-messages
  [room]
  [
   {:from {:local "dialelo"}
    :to "sloth@niwi.be"
    :body "Eso es como Javascript: un accidente histórico"
    :avatar "/static/imgs/placerholder-avatar-1.jpg"}
   {:from {:local "ramiro"}
    :to "sloth@niwi.be"
    :body "No soy grumpy, hago ruiditos."
    :avatar "/static/imgs/placerholder-avatar-2.jpg"}
   {:from {:local "niwibe"}
    :to "sloth@niwi.be"
    :body "¿Puedo hacerle una crítica constructiva a la puta mierda que has hecho?"
    :avatar "/static/imgs/placerholder-avatar-3.jpg"}
   {:from {:local "miguel"}
    :to "sloth@niwi.be"
    :body "Al final nos vamos a comer la mierda de nuestro propio perro"
    :avatar "/static/imgs/placerholder-avatar-4.jpg"}
   ])
; TODO: functions for updating state
