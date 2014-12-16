(defproject openslack "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "BSD (2-Clause)"
            :url "http://opensource.org/licenses/BSD-2-Clause"}
  :dependencies [[org.clojure/clojure "1.7.0-alpha4"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/tools.namespace "0.2.7"]

                 ;; Backend dependencies
                 [jarohen/nomad "0.7.0"]
                 [hiccup "1.0.5"]
                 [compojure "1.3.1"]
                 [com.stuartsierra/component "0.2.2"]
                 [ring/ring-core "1.3.2" :exclusions [javax.servlet/servlet-api
                                                      org.clojure/tools.reader]]
                 [buddy "0.2.3"]
                 [potemkin "0.3.11"]
                 [cc.qbits/jet "0.5.1"]
                 [com.cemerick/piggieback "0.1.3"]
                 [weasel "0.4.2"]

                 ;; XMPP components
                 [org.igniterealtime.smack/smack-core "4.0.6"]
                 [org.igniterealtime.smack/smack-tcp "4.0.6"]
                 [org.igniterealtime.smack/smack-extensions "4.0.6"]
                 [org.igniterealtime.smack/smack-experimental "4.0.6"]

                 ;; Frontend dependencies
                 [org.clojure/clojurescript "0.0-2411"]
                 [secretary "1.2.1"]
                 [sablono "0.2.22" :exclusions [com.facebook/react]]
                 [om "0.8.0-beta3"]
                 [hodgepodge "0.1.0"]

                 ;; Shared dependencies
                 [hiccup-bridge "1.0.1"]
                 [cats "0.2.0" :exclusions [org.clojure/clojure]]]

  :source-paths ["src/clj"]
  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
  :plugins [[lein-cljsbuild "1.0.3"]
            [hiccup-bridge "1.0.1"]]
  :cljsbuild {:builds
              [{:id "devel"
                :source-paths ["src/cljs"]
                :compiler {:output-to "resources/public/js/app.js"
                           :output-dir "resources/public/js"
                           :optimizations :none
                           :source-map true
                           ;; :preamble ["react/react.js"]
                           }}
               {:id "release"
                :source-paths ["src/cljs"]
                :compiler {:output-to "resources/public/js/app.js"
                           :optimizations :advanced
                           :pretty-print false
                           :preamble ["react/react.min.js"]
                           :externs ["react/externs/react.js"]}}]}

  :jar-exclusions [#"user.clj"]
  :target-path "target/%s"
  :jvm-opts ["-Dnomad.env=devel"]
  :profiles {:standalone {:main ^:skip-aot openslack.core}
             :release {:main ^:skip-aot openslack.core
                       :jvm-opts ["-Dnomad.env=release"]}})
