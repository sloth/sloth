(defproject openslack "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "BSD (2-Clause)"
            :url "http://opensource.org/licenses/BSD-2-Clause"}
  :dependencies [[org.clojure/clojure "1.7.0-alpha4"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]

                 ;; Backend dependencies
                 [compojure "1.3.1"]
                 [ring/ring-core "1.3.2" :exclusions [javax.servlet/servlet-api
                                                      org.clojure/tools.reader]]
                 [cc.qbits/jet "0.5.1"]
                 [com.cemerick/piggieback "0.1.3"]
                 [weasel "0.4.2"]

                 ;; Frontend dependencies
                 [org.clojure/clojurescript "0.0-2411"]
                 [secretary "1.2.1"]
                 [sablono "0.2.22" :exclusions [com.facebook/react]]
                 [om "0.8.0-beta3"]
                 [hodgepodge "0.1.0"]]

  :source-paths ["src/clj"]
  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
  :plugins [[lein-cljsbuild "1.0.3"]]
  :cljsbuild {:builds
              [{:id "devel"
                :source-paths ["src/cljs"]
                :compiler {:output-to "resources/public/js/app.js"
                           :output-dir "resources/public/js"
                           :optimizations :none
                           :source-map true
                           ;; :preamble ["react/react.js"]
                           }}]}

  :target-path "target/%s"
  :main ^:skip-aot openslack.core)
