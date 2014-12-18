(ns sloth.web.home
  (:require [compojure.response :refer [render]]
            [hiccup.page :refer [html5 include-js include-css]]))

(defn- render-homepage-html
  []
  (html5
   {:lang "en"}
   [:head
    [:meta {:charset "utf-8"}]
    [:title "Sloth"]
    (include-css "/static/styles/main.css")]
   [:body
    (include-js "http://fb.me/react-0.11.2.js"
                "/static/js/vendor.js"
                "/static/js/goog/base.js"
                "/static/js/app.js")
    [:script "goog.require(\"sloth.core\");"]]))

(defn home-ctrl
  [req]
  (let [html (render-homepage-html)]
    (render html req)))
