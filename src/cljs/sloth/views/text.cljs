(ns sloth.views.text
  (:require [cuerdas.core :as str]
            [sloth.state :as st]
            [sloth.routing :refer [emoji-route room-route contact-route navigate]]
            [clojure.set :refer [subset?]])
  (:import [goog Uri]))

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

(defn enrich-text
  [s]
  (let [result (atom [s])]
    (doseq [f @enrichers]
      (swap! result #(mapcat (fn [v]
                               (if (string? v)
                                 (f v)
                                 [v])) %)))
    (filter (complement empty?) @result)))

(def enrich-text (memoize enrich-text))

(defn make-external-link
  [url]
  [:a {:href url
       :target :blank}
   url])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; String manipulation (maybe for cuerdas?)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn unsurround
  [s]
  (.substring s 1 (dec (.-length s))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Images
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn make-image-message
  [url]
  [:span
   (make-external-link url)
   [:br]
   [:img {:src url
          :class-name "message-image"}]])

(def img-regex #"https?://.*\.(?:jpe?g|gif|png)")

(register-enricher! img-regex make-image-message)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Videos
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn make-video-message
  [url]
  [:span
   (make-external-link url)
   [:br]
   [:video {:src url
            :controls true}]])

(def video-regex #"https?://.*\.(?:webm|mp4|ogv)")

(register-enricher! video-regex make-video-message)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; URLs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def http-url-regex #"https?://[^\s]+")

(defmulti convert-http-url
  (fn [url]
    (keyword (.getDomain (Uri. url)))))

(defmethod convert-http-url :imgur.com
  [url]
  (let [uri (Uri. url)
        rpath (.getPath uri)
        domain "http://i.imgur.com"
        path (if (str/startswith? rpath "/gallery")
               (str "/" (str/ltrim rpath "/gallery"))
               rpath)]
    (make-image-message (str domain path ".jpg"))))

(defmethod convert-http-url :giphy.com
  [url]
  (let [uri (Uri. url)
        rpath (.getPath uri)
        gif-id (last (str/split rpath "-"))
        domain "http://media.giphy.com"
        path (str "/media/" gif-id "/giphy.gif")]
    (make-image-message (str domain path))))

(defmethod convert-http-url :www.youtube.com
  [url]
  (let [uri (Uri. url)]
    (if-let [video-id (.getParameterValue uri "v")]
       [:iframe {:width 560
                 :height 315
                 :frameborder 0
                 :allowfullscreen true
                 :src (str "http://www.youtube.com/embed/" video-id)}]
       (make-external-link url))))

(defmethod convert-http-url :play.spotify.com
  [url]
  (let [uri (Uri. url)
        path (.getPath uri)
        splitted-path (filter (complement empty?) (str/split path "/"))
        url-kind (first splitted-path)
        entity-id (last splitted-path)
        embed-url (str "https://embed.spotify.com/?uri=spotify:")
        iframe-attrs {:frameborder 0
                      :allowtransparency true
                      :height 80}
        default-value (make-external-link url)]
    (condp = url-kind
      "album" [:iframe (assoc iframe-attrs :src (str embed-url "album:" entity-id))]
      "track" [:iframe (assoc iframe-attrs :src (str embed-url "track:" entity-id))]
      "user" (if (subset? #{"user" "playlist"} (set splitted-path))
               (let [username (nth splitted-path 1)
                     src (str embed-url "user:" username ":playlist:" entity-id)]
                 [:iframe (assoc iframe-attrs :src src)])
               default-value)
      default-value)))

(defmethod convert-http-url :www.google.es
  [url]
  (let [uri (Uri. url)
        path (.getPath uri)
        splitted-path (filter (complement empty?) (str/split path "/"))
        url-category (keyword (first splitted-path))]

    (.log js/console "--- MAPA ---")
    (.log js/console (str url-caterogy))
    (if (= url-category :maps)
      (let [embed-url (str "https://maps.googleapis.com/maps/api/staticmap?")
            additional-attrs (str "&zoom=15&size=600x300")
            coords (str/join "," (subvec (str/split (nth (str/split uri "@") 1) ",") 0 2))
            coords-attr (str "center=" coords)
            mark-attr (str "&markers=color:red|label:A|" coords)
            maps-static-url (str embed-url coords-attr mark-attr additional-attrs)]
        (.log js/console "--- DE VERDAD ---")
        (make-image-message maps-static-url))
      (make-external-link url))))

(defmethod convert-http-url :default
  [url]
  (make-external-link url))

(register-enricher! http-url-regex convert-http-url)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Rooms
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def room-regex #"#\w+")
(defn room-converter
  [room-name]
  (let [room-local (str/ltrim room-name "#")
        room-route (room-route {:name room-local})]
    (if-let [maybe-room (st/get-room @st/state room-local)]
      [:a {:href room-route
           :on-click (fn [e]
                       (.preventDefault e)
                       (navigate room-route))}
       room-name]
      [:span room-name])))

(register-enricher! room-regex room-converter)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Contacts
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def contact-regex #"@[^\s]+")
(defn contact-converter
  [contact-name]
  (let [contact-local (str/ltrim contact-name "@")
        contact-url (contact-route {:name contact-local})]
    ;; TODO: if it's the logged-in users name we may want to do something special
    (if-let [maybe-contact (st/get-contact @st/state contact-local)]
      ;; TODO: mention-user class sets background, doesn't look good with links
      [:a {:href contact-url
           :on-click (fn [e]
                       (.preventDefault e)
                       (navigate contact-url))}
       contact-name]
      [:span contact-name])))

(register-enricher! contact-regex contact-converter)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Emoji
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def valid-emojis
  #{"100"
    "1234"
    "-1"
    "+1"
    "8ball"
    "abcd"
    "abc"
    "ab"
    "accept"
    "aerial_tramway"
    "airplane"
    "alarm_clock"
    "alien"
    "ambulance"
    "anchor"
    "angel"
    "anger"
    "angry"
    "anguished"
    "ant"
    "a"
    "apple"
    "aquarius"
    "aries"
    "arrow_backward"
    "arrow_double_down"
    "arrow_double_up"
    "arrow_down"
    "arrow_down_small"
    "arrow_forward"
    "arrow_heading_down"
    "arrow_heading_up"
    "arrow_left"
    "arrow_lower_left"
    "arrow_lower_right"
    "arrow_right_hook"
    "arrow_right"
    "arrows_clockwise"
    "arrows_counterclockwise"
    "arrow_up_down"
    "arrow_upper_left"
    "arrow_upper_right"
    "arrow_up"
    "arrow_up_small"
    "articulated_lorry"
    "art"
    "astonished"
    "atm"
    "baby_bottle"
    "baby_chick"
    "baby"
    "baby_symbol"
    "back"
    "baggage_claim"
    "balloon"
    "ballot_box_with_check"
    "bamboo"
    "banana"
    "bangbang"
    "bank"
    "barber"
    "bar_chart"
    "baseball"
    "basketball"
    "bath"
    "bathtub"
    "battery"
    "bear"
    "bee"
    "beer"
    "beers"
    "beetle"
    "beginner"
    "bell"
    "bento"
    "bicyclist"
    "bike"
    "bikini"
    "bird"
    "birthday"
    "black_circle"
    "black_joker"
    "black_medium_small_square"
    "black_medium_square"
    "black_nib"
    "black_small_square"
    "black_square_button"
    "black_square"
    "blossom"
    "blowfish"
    "blue_book"
    "blue_car"
    "blue_heart"
    "blush"
    "boar"
    "boat"
    "bomb"
    "bookmark"
    "bookmark_tabs"
    "book"
    "books"
    "boom"
    "boot"
    "bouquet"
    "bowling"
    "bow"
    "bowtie"
    "boy"
    "b"
    "bread"
    "bride_with_veil"
    "bridge_at_night"
    "briefcase"
    "broken_heart"
    "bug"
    "bulb"
    "bullettrain_front"
    "bullettrain_side"
    "bus"
    "busstop"
    "bust_in_silhouette"
    "busts_in_silhouette"
    "cactus"
    "cake"
    "calendar"
    "calling"
    "camel"
    "camera"
    "cancer"
    "candy"
    "capital_abcd"
    "capricorn"
    "card_index"
    "carousel_horse"
    "car"
    "cat2"
    "cat"
    "cd"
    "chart"
    "chart_with_downwards_trend"
    "chart_with_upwards_trend"
    "checkered_flag"
    "cherries"
    "cherry_blossom"
    "chestnut"
    "chicken"
    "children_crossing"
    "chocolate_bar"
    "christmas_tree"
    "church"
    "cinema"
    "circus_tent"
    "city_sunrise"
    "city_sunset"
    "clapper"
    "clap"
    "clipboard"
    "clock1030"
    "clock10"
    "clock1130"
    "clock11"
    "clock1230"
    "clock12"
    "clock130"
    "clock1"
    "clock230"
    "clock2"
    "clock330"
    "clock3"
    "clock430"
    "clock4"
    "clock530"
    "clock5"
    "clock630"
    "clock6"
    "clock730"
    "clock7"
    "clock830"
    "clock8"
    "clock930"
    "clock9"
    "closed_book"
    "closed_lock_with_key"
    "closed_umbrella"
    "cloud"
    "cl"
    "clubs"
    "cn"
    "cocktail"
    "coffee"
    "cold_sweat"
    "collision"
    "computer"
    "confetti_ball"
    "confounded"
    "confused"
    "congratulations"
    "construction"
    "construction_worker"
    "convenience_store"
    "cookie"
    "cool"
    "cop"
    "copyright"
    "corn"
    "couplekiss"
    "couple"
    "couple_with_heart"
    "cow2"
    "cow"
    "credit_card"
    "crocodile"
    "crossed_flags"
    "crown"
    "crying_cat_face"
    "cry"
    "crystal_ball"
    "cupid"
    "curly_loop"
    "currency_exchange"
    "curry"
    "custard"
    "customs"
    "cyclone"
    "dancer"
    "dancers"
    "dango"
    "dart"
    "dash"
    "date"
    "deciduous_tree"
    "department_store"
    "de"
    "diamond_shape_with_a_dot_inside"
    "diamonds"
    "disappointed"
    "disappointed_relieved"
    "dizzy_face"
    "dizzy"
    "dog2"
    "dog"
    "dollar"
    "dolls"
    "dolphin"
    "do_not_litter"
    "donut"
    "door"
    "doughnut"
    "dragon_face"
    "dragon"
    "dress"
    "dromedary_camel"
    "droplet"
    "dvd"
    "ear_of_rice"
    "ear"
    "earth_africa"
    "earth_americas"
    "earth_asia"
    "eggplant"
    "egg"
    "eight"
    "eight_pointed_black_star"
    "eight_spoked_asterisk"
    "electric_plug"
    "elephant"
    "e-mail"
    "email"
    "end"
    "envelope"
    "es"
    "european_castle"
    "european_post_office"
    "euro"
    "evergreen_tree"
    "exclamation"
    "expressionless"
    "eyeglasses"
    "eyes"
    "facepunch"
    "factory"
    "fallen_leaf"
    "family"
    "fast_forward"
    "fax"
    "fearful"
    "feelsgood"
    "feet"
    "ferris_wheel"
    "file_folder"
    "finnadie"
    "fire_engine"
    "fire"
    "fireworks"
    "first_quarter_moon"
    "first_quarter_moon_with_face"
    "fish_cake"
    "fishing_pole_and_fish"
    "fish"
    "fist"
    "five"
    "flags"
    "flashlight"
    "floppy_disk"
    "flower_playing_cards"
    "flushed"
    "foggy"
    "football"
    "fork_and_knife"
    "fountain"
    "four_leaf_clover"
    "four"
    "free"
    "fried_shrimp"
    "fries"
    "frog"
    "frowning"
    "fr"
    "fuelpump"
    "full_moon"
    "full_moon_with_face"
    "fu"
    "game_die"
    "gb"
    "gemini"
    "gem"
    "ghost"
    "gift_heart"
    "gift"
    "girl"
    "globe_with_meridians"
    "goat"
    "goberserk"
    "godmode"
    "golf"
    "grapes"
    "green_apple"
    "green_book"
    "green_heart"
    "grey_exclamation"
    "grey_question"
    "grimacing"
    "grinning"
    "grin"
    "guardsman"
    "guitar"
    "gun"
    "haircut"
    "hamburger"
    "hammer"
    "hamster"
    "handbag"
    "hand"
    "hankey"
    "hash"
    "hatched_chick"
    "hatching_chick"
    "headphones"
    "hear_no_evil"
    "heartbeat"
    "heart_decoration"
    "heart_eyes_cat"
    "heart_eyes"
    "heart"
    "heartpulse"
    "hearts"
    "heavy_check_mark"
    "heavy_division_sign"
    "heavy_dollar_sign"
    "heavy_exclamation_mark"
    "heavy_minus_sign"
    "heavy_multiplication_x"
    "heavy_plus_sign"
    "helicopter"
    "herb"
    "hibiscus"
    "high_brightness"
    "high_heel"
    "hocho"
    "honeybee"
    "honey_pot"
    "horse"
    "horse_racing"
    "hospital"
    "hotel"
    "hotsprings"
    "hourglass_flowing_sand"
    "hourglass"
    "house"
    "house_with_garden"
    "hurtrealbad"
    "hushed"
    "ice_cream"
    "icecream"
    "ideograph_advantage"
    "id"
    "imp"
    "inbox_tray"
    "incoming_envelope"
    "information_desk_person"
    "information_source"
    "innocent"
    "interrobang"
    "iphone"
    "it"
    "izakaya_lantern"
    "jack_o_lantern"
    "japanese_castle"
    "japanese_goblin"
    "japanese_ogre"
    "japan"
    "jeans"
    "joy_cat"
    "joy"
    "jp"
    "keycap_ten"
    "key"
    "kimono"
    "kissing_cat"
    "kissing_closed_eyes"
    "kissing_face"
    "kissing_heart"
    "kissing"
    "kissing_smiling_eyes"
    "kiss"
    "koala"
    "koko"
    "kr"
    "large_blue_circle"
    "large_blue_diamond"
    "large_orange_diamond"
    "last_quarter_moon"
    "last_quarter_moon_with_face"
    "laughing"
    "leaves"
    "ledger"
    "left_luggage"
    "left_right_arrow"
    "leftwards_arrow_with_hook"
    "lemon"
    "leopard"
    "leo"
    "libra"
    "light_rail"
    "link"
    "lips"
    "lipstick"
    "lock"
    "lock_with_ink_pen"
    "lollipop"
    "loop"
    "loudspeaker"
    "love_hotel"
    "love_letter"
    "low_brightness"
    "mag"
    "mag_right"
    "mahjong"
    "mailbox_closed"
    "mailbox"
    "mailbox_with_mail"
    "mailbox_with_no_mail"
    "man"
    "mans_shoe"
    "man_with_gua_pi_mao"
    "man_with_turban"
    "maple_leaf"
    "mask"
    "massage"
    "meat_on_bone"
    "mega"
    "melon"
    "memo"
    "mens"
    "metal"
    "metro"
    "microphone"
    "microscope"
    "milky_way"
    "minibus"
    "minidisc"
    "mobile_phone_off"
    "moneybag"
    "money_with_wings"
    "monkey_face"
    "monkey"
    "monorail"
    "moon"
    "mortar_board"
    "mountain_bicyclist"
    "mountain_cableway"
    "mountain_railway"
    "mount_fuji"
    "mouse2"
    "mouse"
    "movie_camera"
    "moyai"
    "m"
    "muscle"
    "mushroom"
    "musical_keyboard"
    "musical_note"
    "musical_score"
    "mute"
    "nail_care"
    "name_badge"
    "neckbeard"
    "necktie"
    "negative_squared_cross_mark"
    "neutral_face"
    "new_moon"
    "new_moon_with_face"
    "new"
    "newspaper"
    "ng"
    "nine"
    "no_bell"
    "no_bicycles"
    "no_entry"
    "no_entry_sign"
    "no_good"
    "no_mobile_phones"
    "no_mouth"
    "non-potable_water"
    "no_pedestrians"
    "nose"
    "no_smoking"
    "notebook"
    "notebook_with_decorative_cover"
    "notes"
    "nut_and_bolt"
    "o2"
    "ocean"
    "octocat"
    "octopus"
    "oden"
    "office"
    "ok_hand"
    "ok"
    "ok_woman"
    "older_man"
    "older_woman"
    "oncoming_automobile"
    "oncoming_bus"
    "oncoming_police_car"
    "oncoming_taxi"
    "one"
    "on"
    "open_file_folder"
    "open_hands"
    "open_mouth"
    "ophiuchus"
    "o"
    "orange_book"
    "outbox_tray"
    "ox"
    "package"
    "page_facing_up"
    "pager"
    "page_with_curl"
    "palm_tree"
    "panda_face"
    "paperclip"
    "parking"
    "part_alternation_mark"
    "partly_sunny"
    "passport_control"
    "paw_prints"
    "peach"
    "pear"
    "pencil2"
    "pencil"
    "penguin"
    "pensive"
    "performing_arts"
    "persevere"
    "person_frowning"
    "person_with_blond_hair"
    "person_with_pouting_face"
    "phone"
    "pig2"
    "pig_nose"
    "pig"
    "pill"
    "pineapple"
    "pisces"
    "pizza"
    "plus1"
    "point_down"
    "point_left"
    "point_right"
    "point_up_2"
    "point_up"
    "police_car"
    "poodle"
    "poop"
    "postal_horn"
    "postbox"
    "post_office"
    "potable_water"
    "pouch"
    "poultry_leg"
    "pound"
    "pouting_cat"
    "pray"
    "princess"
    "punch"
    "purple_heart"
    "purse"
    "pushpin"
    "put_litter_in_its_place"
    "question"
    "rabbit2"
    "rabbit"
    "racehorse"
    "radio_button"
    "radio"
    "rage1"
    "rage2"
    "rage3"
    "rage4"
    "rage"
    "railway_car"
    "rainbow"
    "raised_hand"
    "raised_hands"
    "raising_hand"
    "ramen"
    "ram"
    "rat"
    "recycle"
    "red_car"
    "red_circle"
    "registered"
    "relaxed"
    "relieved"
    "repeat_one"
    "repeat"
    "restroom"
    "revolving_hearts"
    "rewind"
    "ribbon"
    "rice_ball"
    "rice_cracker"
    "rice"
    "rice_scene"
    "ring"
    "rocket"
    "roller_coaster"
    "rooster"
    "rose"
    "rotating_light"
    "round_pushpin"
    "rowboat"
    "rugby_football"
    "runner"
    "running"
    "running_shirt_with_sash"
    "ru"
    "sagittarius"
    "sailboat"
    "sake"
    "sandal"
    "santa"
    "sa"
    "satellite"
    "satisfied"
    "saxophone"
    "school"
    "school_satchel"
    "scissors"
    "scorpius"
    "scream_cat"
    "scream"
    "scroll"
    "seat"
    "secret"
    "seedling"
    "see_no_evil"
    "seven"
    "shaved_ice"
    "sheep"
    "shell"
    "shipit"
    "ship"
    "shirt"
    "shit"
    "shoe"
    "shower"
    "signal_strength"
    "six"
    "six_pointed_star"
    "ski"
    "skull"
    "sleeping"
    "sleepy"
    "slot_machine"
    "small_blue_diamond"
    "small_orange_diamond"
    "small_red_triangle_down"
    "small_red_triangle"
    "smile_cat"
    "smile"
    "smiley_cat"
    "smiley"
    "smiling_imp"
    "smirk_cat"
    "smirk"
    "smoking"
    "snail"
    "snake"
    "snowboarder"
    "snowflake"
    "snowman"
    "sob"
    "soccer"
    "soon"
    "sos"
    "sound"
    "space_invader"
    "spades"
    "spaghetti"
    "sparkle"
    "sparkler"
    "sparkles"
    "sparkling_heart"
    "speaker"
    "speak_no_evil"
    "speech_balloon"
    "speedboat"
    "squirrel"
    "star2"
    "star"
    "stars"
    "station"
    "statue_of_liberty"
    "steam_locomotive"
    "stew"
    "straight_ruler"
    "strawberry"
    "stuck_out_tongue_closed_eyes"
    "stuck_out_tongue"
    "stuck_out_tongue_winking_eye"
    "sunflower"
    "sunglasses"
    "sunny"
    "sunrise_over_mountains"
    "sunrise"
    "sun_with_face"
    "surfer"
    "sushi"
    "suspect"
    "suspension_railway"
    "sweat_drops"
    "sweat"
    "sweat_smile"
    "sweet_potato"
    "swimmer"
    "symbols"
    "syringe"
    "tada"
    "tanabata_tree"
    "tangerine"
    "taurus"
    "taxi"
    "tea"
    "telephone"
    "telephone_receiver"
    "telescope"
    "tennis"
    "tent"
    "thought_balloon"
    "three"
    "thumbsdown"
    "thumbsup"
    "ticket"
    "tiger2"
    "tiger"
    "tired_face"
    "tm"
    "toilet"
    "tokyo_tower"
    "tomato"
    "tongue"
    "tophat"
    "top"
    "tractor"
    "traffic_light"
    "train2"
    "train"
    "tram"
    "triangular_flag_on_post"
    "triangular_ruler"
    "trident"
    "triumph"
    "trolleybus"
    "trollface"
    "trophy"
    "tropical_drink"
    "tropical_fish"
    "truck"
    "trumpet"
    "tshirt"
    "tulip"
    "turtle"
    "tv"
    "twisted_rightwards_arrows"
    "two_hearts"
    "two_men_holding_hands"
    "two"
    "two_women_holding_hands"
    "u5272"
    "u5408"
    "u55b6"
    "u6307"
    "u6708"
    "u6709"
    "u6e80"
    "u7121"
    "u7533"
    "u7981"
    "u7a7a"
    "uk"
    "umbrella"
    "unamused"
    "underage"
    "unlock"
    "up"
    "us"
    "vertical_traffic_light"
    "vhs"
    "vibration_mode"
    "video_camera"
    "video_game"
    "violin"
    "virgo"
    "volcano"
    "v"
    "vs"
    "walking"
    "waning_crescent_moon"
    "waning_gibbous_moon"
    "warning"
    "watch"
    "water_buffalo"
    "watermelon"
    "wave"
    "wavy_dash"
    "waxing_crescent_moon"
    "waxing_gibbous_moon"
    "wc"
    "weary"
    "wedding"
    "whale2"
    "whale"
    "wheelchair"
    "white_check_mark"
    "white_circle"
    "white_flower"
    "white_large_square"
    "white_medium_small_square"
    "white_medium_square"
    "white_small_square"
    "white_square_button"
    "wind_chime"
    "wine_glass"
    "wink"
    "wolf"
    "woman"
    "womans_clothes"
    "womans_hat"
    "womens"
    "worried"
    "wrench"
    "x"
    "yellow_heart"
    "yen"
    "yum"
    "zap"
    "zero"
    "zzz"})

(def emoji-regex #"\:[^:\s]+\:")
(defn emoji-converter
  [e]
  (let [emoji (unsurround e)]
    (if (valid-emojis emoji)
      [:img {:src (emoji-route emoji)
             :class-name "emoji"}]
      e)))

(register-enricher! emoji-regex emoji-converter)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Markup
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(def bold-regex #"\*[^*\n]+\*")
(defn bold-converter
  [s]
  [:strong (unsurround s)])

(register-enricher! bold-regex bold-converter)

(def cursive-regex #"/[^/\n]+/")
(defn cursive-converter
  [s]
  [:em (unsurround s)])

(register-enricher! cursive-regex cursive-converter)

(def cross-out-regex #"-[^/\n]+-")
(defn cross-out-converter
  [s]
  [:i.cross-out (unsurround s)])

(register-enricher! cross-out-regex cross-out-converter)
