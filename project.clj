(defproject sloth "0.1.1-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "BSD (2-Clause)"
            :url "http://opensource.org/licenses/BSD-2-Clause"}

  :dependencies [[org.clojure/clojure "1.6.0" :scope "provided"]
                 [org.clojure/clojurescript "0.0-2665" :scope "provided"]
                 [org.clojure/tools.namespace "0.2.7"]

                 ;; Backend dependencies
                 [compojure "1.3.1"]
                 [cc.qbits/jet "0.5.1"]
                 [ring/ring-core "1.3.2" :exclusions [javax.servlet/servlet-api
                                                      org.clojure/tools.reader]]

                 ;; Frontend dependencies
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]

                 ;; [com.facebook/react "0.12.2.1"]
                 [org.om/om "0.8.0" :exclusions [org.clojure/clojurescript]]
                 [secretary "1.2.1" :exclusions [org.clojure/clojurescript]]
                 [sablono "0.3.0-SNAPSHOT" :exclusions [com.facebook/react org.clojure/clojurescript]]
                 [hodgepodge "0.1.2"]
                 [cats "0.2.0" :exclusions [org.clojure/clojure
                                            org.clojure/clojurescript]]
                 [cuerdas "0.1.0" :exclusions [org.clojure/clojurescript]]
                 [shodan "0.4.1" :exclusions [org.clojure/clojurescript]]

                 ;; Commented due to temporary compatibility issue with the
                 ;; last clojurescript compiler version.
                 ;; [com.cemerick/piggieback "0.1.3"]
                 ;; [weasel "0.4.2"]

                 ;; ;; Shared dependencies
                 [hiccup-bridge "1.0.1" :exclusions [org.clojure/clojurescript]]]

  :source-paths ["src/clj"]
  :jar-exclusions [#"user.clj"]
  :target-path "target/%s"

  ;; Commented due to temporary compatibility issue with the
  ;; last clojurescript compiler version.
  ;; :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

  :clean-targets ^{:protect false} ["target" "resources/public/js/app.js"
                                    "resources/public/js/out/"]

  :plugins [[lein-cljsbuild "1.0.4" :exclusions [org.clojure/clojure]]
            [hiccup-bridge "1.0.1" :exclusions [org.clojure/clojurescript]]]

  :cljsbuild {:builds
              [{:id "devel"
                :source-paths ["src/cljs"]
                :compiler {:output-to "resources/public/js/app.js"
                           :output-dir "resources/public/js/out"
                           :optimizations :none
                           :source-map true}}

               {:id "normal"
                :source-paths ["src/cljs"]
                :compiler {:output-to "resources/public/js/app.js"
                           :optimizations :whitespace
                           :static-fns true
                           :pretty-print true}}]}

  :profiles {:standalone {:main ^:skip-aot sloth.core}})

