; TODO: List of valid emojis
(ns openslack.text
  (:require [cuerdas.core :as str]))

(def enrichers (atom []))

(defn make-enricher
  [regex match-converter]
  (fn [s]
    (let [matches (transient [])
          _ (str/replace s regex (fn [m] (conj! matches m)))
          splitted (str/split s regex)]
      (interleave splitted
                  (concat (map match-converter (persistent! matches)) [""])))))

(defn register-enricher!
  [regex match-converter]
  (swap! enrichers conj (make-enricher regex match-converter)))

(def img-regex #"https?://.*\.(?:jpe?g|gif|png)")
(def img-converter (fn [e]
                     [:img {:src e
                            :class-name "message-image"}]))
(register-enricher! img-regex img-converter)

(def emoji-regex #"\:[^\\s:]+\:")
(def emoji-converter (fn [e]
                       [:img {:src (str "/static/imgs/emoji/"
                                        (.substring e 1 (dec  (.-length e)))
                                        ".png")
                              :class-name "emoji"}]))
(register-enricher! emoji-regex emoji-converter)

(defn enrich-text
  [s]
  (let [result (atom [s])]
    (doseq [f @enrichers]
      (swap! result #(mapcat (fn [v]
                               (if (string? v)
                                 (f v)
                                 [v])) %)))
    (filter (complement empty?) @result)))
