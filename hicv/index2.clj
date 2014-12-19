("<!doctype html>"
 [:html
  {:lang "en"}
  [:head
   [:meta {:charset "utf-8"}]
   [:title "Open Sloth"]
   [:meta {:content "Open Sloth", :name "description"}]
   [:link {:href "/static/styles/main.css", :rel "stylesheet"}]]
  [:body
   [:section#app.client
    [:div.client-sidebar-holder
     [:div.client-sidebar
      [:div.client-lists
       [:div.logo "SlothLogo"]
       [:div.room-list.sidebar-list
        [:h3 "Channels"]
        [:ul
         [:li.unread
          [:span "#"]
          "SlothMyMachine"
          [:i "3"]
          [:i.close-channel "x"]]
         [:li.unread
          [:span "#"]
          "SlothThisShit"
          [:i "4"]
          [:i.close-channel "x"]]
         [:li [:span "#"] "SlothThugLife" [:i.close-channel "x"]]]]
       [:hr]
       [:div.room-list.sidebar-list
        [:h3.nohover "Invited to this channels"]
        [:ul
         [:li.invited
          [:span "#"]
          "SlothOnElorrio"
          [:i "!"]
          [:p "By " [:a {:href "#"} "@dialelo"]]]
         [:li.invited
          [:span "#"]
          "SlothAreBadass"
          [:i "!"]
          [:p "By " [:a {:href "#"} "@mgdelacroix"]]]
         [:li.invited
          [:span "#"]
          "SlothNucelarSurvivor"
          [:i "!"]
          [:p "By " [:a {:href "#"} "@rsanchezbalo"]]]
         [:li.invited
          [:span "#"]
          "Slothslother"
          [:i "!"]
          [:p "By " [:a {:href "#"} "@niwibe"]]]]]
       [:hr]
       [:div.room-list.sidebar-list
        [:h3 "Contact List"]
        [:ul
         [:li.unread
          [:span.status.online]
          "SlothMyMachine"
          [:i "3"]
          [:div.read-status "Lorem ipsum dolor sit amet."]]
         [:li.unread
          [:span.status.busy]
          "SlothThisShit"
          [:i "4"]
          [:div.read-status "Lorem ipsum dolor sit amet."]]
         [:li
          [:span.status.offline]
          "SlothThugLife\n                "
          [:div.read-status "Lorem ipsum dolor sit amet."]]]]]
      [:div.active-user
       [:img
        {:height "50",
         :width "50",
         :alt "#user",
         :src "/static/imgs/placerholder-avatar-1.jpg"}]
       [:div.square
        [:div.row [:h2 "Slothmachine"]]
        [:div.row
         [:div.status.online]
         " "
         "<!-- #ESTADOS:Online, Offline, Busy -->"
         [:p.status-text "Tralari tralara, traliron pompan"]]]]]]
    [:section.client-main
     [:div.sloth-main
      [:div.sloth-overflow.bounceInUp
       [:div.sloth-presentation
        [:div.text
         [:h1 "Welcome to Sloth"]
         [:h2
          "My name is Slotherin and I'm going to explain some cool features about Sloth"]
         [:p
          "· Sloth brings all your team communication into one place, makes it all instantly searchable and available wherever you go."]
         [:p
          "· Our aim is to make your working life simpler, more pleasant and more productive."]
         [:p
          "· Communication  in sloth happens in public channels, direct messages and private groups."]
         [:p
          "· Everything is indexed, archived and synced across devices so you can always pick up exactly where you left off."]
         [:p.last
          "\n                When you're ready, "
          [:a {:href "#"} "invite your teammates to sloth"]
          ".\n              "]]
        [:img {:src "/static/imgs/slotherin.png"}]]]]]]]])
