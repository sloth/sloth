(ns openslack.views
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [openslack.views.login :refer [login]]
            [openslack.views.sidebar :refer [sidebar]]))
;            [openslack.views.main :refer [main]]))


(def main [:section.client-main
     [:header
      [:h1 "#SlothMyMachine"]
      [:h2
       "Le topic del dia: Los "
       [:strong "Sloth"]
       " dominaran el mundo\n        "]]
     [:hr]
     [:div.chat-zone
      [:div.chat-container
       [:div.messages-container
        [:div.message
         [:div.message-avatar
          [:img
           {:height "35",
            :width "35",
            :alt "#user",
            :src "/static/imgs/placerholder-avatar-1.jpg"}]]
         [:div.message-content
          [:div.message-title [:strong "dialelo"] [:span "10:11 pm"]]
          [:p.content "Lorem ipsum dolor sit.\n                  "]]]
        [:div.message
         [:div.message-avatar
          [:img
           {:height "35",
            :width "35",
            :alt "#user",
            :src "/static/imgs/placerholder-avatar-2.jpg"}]]
         [:div.message-content
          [:div.message-title [:strong "Ramiro"] [:span "10:30 pm"]]
          [:p.content
           "  Lorem ipsum dolor sit amet, consectetur adipisicing elit. Pariatur ab reiciendis ducimus, iure ad, blanditiis rerum nobis, laboriosam quae repudiandae atque!\n                  "]]]
        [:div.message
         [:div.message-avatar
          [:img
           {:height "35",
            :width "35",
            :alt "#user",
            :src "/static/imgs/placerholder-avatar-1.jpg"}]]
         [:div.message-content
          [:div.message-title [:strong "dialelo"] [:span "11:30 pm"]]
          [:p.content
           "Lorem ipsum dolor sit amet, consectetur adipisicing elit. Necessitatibus enim est sapiente ratione impedit!\n                  "]]]
        [:div.message
         [:div.message-avatar
          [:img
           {:height "35",
            :width "35",
            :alt "#user",
            :src "/static/imgs/placerholder-avatar-1.jpg"}]]
         [:div.message-content
          [:div.message-title [:strong "dialelo"] [:span "10:11 pm"]]
          [:p.content "Lorem ipsum dolor sit.\n                  "]]]
        [:div.message
         [:div.message-avatar
          [:img
           {:height "35",
            :width "35",
            :alt "#user",
            :src "/static/imgs/placerholder-avatar-2.jpg"}]]
         [:div.message-content
          [:div.message-title [:strong "Ramiro"] [:span "10:30 pm"]]
          [:p.content
           "  Lorem ipsum dolor sit amet, consectetur adipisicing elit. Pariatur ab reiciendis ducimus, iure ad, blanditiis rerum nobis, laboriosam quae repudiandae atque!\n                  "]]]
        [:div.message
         [:div.message-avatar
          [:img
           {:height "35",
            :width "35",
            :alt "#user",
            :src "/static/imgs/placerholder-avatar-1.jpg"}]]
         [:div.message-content
          [:div.message-title [:strong "dialelo"] [:span "11:30 pm"]]
          [:p.content
           "Lorem ipsum dolor sit amet, consectetur adipisicing elit. Necessitatibus enim est sapiente ratione impedit!\n                  "]]]
        [:div.message
         [:div.message-avatar
          [:img
           {:height "35",
            :width "35",
            :alt "#user",
            :src "/static/imgs/placerholder-avatar-1.jpg"}]]
         [:div.message-content
          [:div.message-title [:strong "dialelo"] [:span "10:11 pm"]]
          [:p.content "Lorem ipsum dolor sit.\n                  "]]]
        [:div.message
         [:div.message-avatar
          [:img
           {:height "35",
            :width "35",
            :alt "#user",
            :src "/static/imgs/placerholder-avatar-2.jpg"}]]
         [:div.message-content
          [:div.message-title [:strong "Ramiro"] [:span "10:30 pm"]]
          [:p.content
           "  Lorem ipsum dolor sit amet, consectetur adipisicing elit. Pariatur ab reiciendis ducimus, iure ad, blanditiis rerum nobis, laboriosam quae repudiandae atque!\n                  "]]]
        [:div.message
         [:div.message-avatar
          [:img
           {:height "35",
            :width "35",
            :alt "#user",
            :src "/static/imgs/placerholder-avatar-1.jpg"}]]
         [:div.message-content
          [:div.message-title [:strong "dialelo"] [:span "11:30 pm"]]
          [:p.content
           "Lorem ipsum dolor sit amet, consectetur adipisicing elit. Necessitatibus enim est sapiente ratione impedit!\n                  "]]]
        [:div.message.highlighted
         [:div.message-avatar
          [:img
           {:height "35",
            :width "35",
            :alt "#user",
            :src "/static/imgs/placerholder-avatar-3.jpg"}]]
         [:div.message-content
          [:div.message-title [:strong "Niwibe"] [:span "12:30 pm"]]
          [:p.content
           "Lorem ipsum dolor sit amet, "
           [:strong "consectetur adipisicing"]
           " elit. Vero impedit mollitia laudantium at eos ipsa consectetur voluptatibus id. Inventore eum accusantium earum aliquid ea quidem debitis officiis dolorem. Numquam nemo veritatis placeat molestiae vitae.\n                  "]
          [:p.content
           "Lorem ipsum dolor sit amet, "
           [:em "consectetur adipisicing elit."]
           " Necessitatibus enim est sapiente ratione impedit!\n                  "]]]
        [:div.message
         [:div.message-avatar
          [:img
           {:height "35",
            :width "35",
            :alt "#user",
            :src "/static/imgs/placerholder-avatar-2.jpg"}]]
         [:div.message-content
          [:div.message-title [:strong "Ramiro"] [:span "10:30 pm"]]
          [:p.content
           "  Lorem ipsum dolor sit amet, consectetur adipisicing elit. Pariatur ab reiciendis ducimus, iure ad, blanditiis rerum nobis, laboriosam quae repudiandae atque!\n                  "]]]
        [:div.message
         [:div.message-avatar
          [:img
           {:height "35",
            :width "35",
            :alt "#user",
            :src "/static/imgs/placerholder-avatar-1.jpg"}]]
         [:div.message-content
          [:div.message-title [:strong "dialelo"] [:span "11:30 pm"]]
          [:p.content
           "Lorem ipsum dolor sit amet, consectetur adipisicing elit. Necessitatibus enim est sapiente ratione impedit!\n                  "]]]
        [:div.message
         [:div.message-avatar
          [:img
           {:height "35",
            :width "35",
            :alt "#user",
            :src "/static/imgs/placerholder-avatar-2.jpg"}]]
         [:div.message-content
          [:div.message-title [:strong "Ramiro"] [:span "10:30 pm"]]
          [:p.content
           "  Lorem ipsum dolor sit amet, consectetur adipisicing elit. Pariatur ab reiciendis ducimus, iure ad, blanditiis rerum nobis, laboriosam quae repudiandae atque!\n                  "]]]
        [:div.message
         [:div.message-avatar
          [:img
           {:height "35",
            :width "35",
            :alt "#user",
            :src "/static/imgs/placerholder-avatar-1.jpg"}]]
         [:div.message-content
          [:div.message-title [:strong "dialelo"] [:span "11:30 pm"]]
          [:p.content
           "Lorem ipsum dolor sit amet, consectetur adipisicing elit. Necessitatibus enim est sapiente ratione impedit!\n                  "]]]
        [:div.message
         [:div.message-avatar
          [:img
           {:height "35",
            :width "35",
            :alt "#user",
            :src "/static/imgs/placerholder-avatar-2.jpg"}]]
         [:div.message-content
          [:div.message-title [:strong "Ramiro"] [:span "10:30 pm"]]
          [:p.content
           "  Lorem ipsum dolor sit amet, consectetur adipisicing elit. Pariatur ab reiciendis ducimus, iure ad, blanditiis rerum nobis, laboriosam quae repudiandae atque!\n                  "]]]
        [:div.message
         [:div.message-avatar
          [:img
           {:height "35",
            :width "35",
            :alt "#user",
            :src "/static/imgs/placerholder-avatar-1.jpg"}]]
         [:div.message-content
          [:div.message-title [:strong "dialelo"] [:span "11:30 pm"]]
          [:p.content
           "Lorem ipsum dolor sit amet, consectetur adipisicing elit. Necessitatibus enim est sapiente ratione impedit!\n                  "]]]]
       [:div.write-message [:textarea " "] [:button "Send"]]]
      [:div.chat-sidebar-holder [:div]]]])

(defn app [state owner]
  (reify
    om/IDisplayName
    (display-name [_] "Sloth")

    om/IRender
    (render [_]
      (html (condp = (get-in state [:page :name])
              :login (om/build login state)
              :home [:section#app.client [:div.client-sidebar-holder (om/build sidebar state)] main]
              :room [:section#app.client [:div.client-sidebar-holder sb] main]
              :contact [:section#app.client [:div.client-sidebar-holder sb] main]
              nil)))))
