(defproject org.clojars.thisdotrob/huckleberry "0.2.1"
  :url "https://github.com/thisdotrob/huckleberry"
  :description "maven dependecy resolution in clojurescript"
  :min-lein-version "2.5.3"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.293"]
                 [andare "0.4.0"]]

  :plugins [[lein-cljsbuild "1.1.3"]
            [lein-cljfmt "0.5.3"]

            [lein-npm "0.6.2"]]

  :source-paths ["src/main/clojure"]
  :test-paths ["src/test/clojure" "src/main/clojure"]

  :clean-targets ["main.js" "out" "target"]

  :main "out/main.js" ;; downloads npm deps and run

  :npm {
        :dependencies [[xml2js "0.4.17"]
                       [request "2.74.0"]]

        }

  :cljsbuild {
              :builds [
                       {:id "dev"
                        :source-paths ["src/main/clojure" "src/dev/clojure"]
                        :figwheel true
                        :compiler {
                                   :main thisdotrob.huckleberry.runner
                                   :output-to "out/main.js"
                                   :target :nodejs
                                   :output-dir "out"
                                   :optimizations :none
                                   :parallel-build true
                                   :source-map true}}
                       {:id "test"
                        :source-paths[ "src/main/clojure" "src/test/clojure"]
                        :compiler {
                                   :main thisdotrob.huckleberry.test-runner
                                   :output-to "out/test.js"
                                   :target :nodejs
                                   :output-dir "out/test"
                                   :optimizations :none
                                   :parallel-build true
                                   :source-map true}
                        }
                       ]}
  :figwheel {}
  :profiles {:dev {:dependencies [[com.cemerick/piggieback "0.2.1"]
                                  [org.clojure/tools.nrepl "0.2.12"]]
                   :plugins [[lein-doo "0.1.7"]
                             [lein-figwheel "0.5.8" :exclusions [cider/cider-nrepl]]]
                   :npm {:dependencies [[ws "1.1.1"]]}}})
