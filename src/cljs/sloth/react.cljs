(ns sloth.react
  "Reactjs and Rum abstraction layer."
  (:require [rum :include-macros true]
            [shodan.console :as console :include-macros true]))

(defn component
  "Build a rum component from clojure hashmap.

  It should consist in a simple map with render function.
  (def mycomponent
    (rum/component
     {:render (fn []
                (s/html [:span \"hello world\"]))}))

  The render function is the unique mandatory function."
  [compmap]
  (let [mixins (get compmap :mixins [])
        mainmixin (cond
                    (:render compmap)
                    (let [renderfn (:render compmap)]
                      (rum/render->mixin renderfn))

                    (:render-state compmap)
                    (let [renderfn (:render-state compmap)]
                      (rum/render-state->mixin renderfn)))
        compmap   (dissoc compmap :render-state :render :mixins)
        clazz     (rum/build-class (concat [mainmixin] [compmap] mixins))]
     (fn [& args]
       (let [state (rum/args->state args)]
         (rum/element clazz state nil)))))

(def mount rum/mount)
(def request-render rum/request-render)

(defn get-component
  [owner]
  (:rum/react-component owner))

;; mixins
(def static rum/static)

(def form-mixin
  {:init (fn [owner] (assoc owner :form (atom {:kaka 1})))
   :will-mount (fn [owner]
                 (console/log "will-mount1" (pr-str owner))
                 (let [comp (get-component owner)
                       form (:form owner)]
                   (console/log "will-mount2" (pr-str form))
                   (add-watch form :form-mixin
                              (fn [_ _ _ v]
                                (console/log "watch" (pr-str v))
                                (request-render comp)))
                   (assoc owner :form form)))
   :will-unmount (fn [owner]
                   (console/log "will-unmount" (pr-str owner))
                   (let [form (:form owner)]
                     (remove-watch form :form-mixin)))})
