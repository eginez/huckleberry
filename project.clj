(defproject org.eginez/huckleberry "0.1.0"
  :url "https://github.com/eginez/huckleberry"
  :description "maven dependecy resolution in clojurescript"
  :min-lein-version "2.5.3"

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.229"]
                 [org.clojure/core.async "0.2.391"]]

  :plugins [[lein-cljsbuild "1.1.3"]
            [lein-cljfmt "0.5.3"]
            [lein-doo "0.1.7"]
            [lein-npm "0.6.2"]]

  :source-paths ["src"]

  :clean-targets ["main.js"
                  "target"]

  :npm {
        :dependencies [[xml2js "0.4.17"]
                       [request "2.74.0"]]

        }

  :cljsbuild {
              :builds [
                       {:id "dev"
                        :source-paths ["src/main/clojure"]
                        :figwheel true
                        :compiler {
                                   :main eginez.huckleberry.core
                                   :output-to "out/main.js"
                                   :target :nodejs
                                   :output-dir "out"
                                   :optimizations :none
                                   :parallel-build true
                                   :source-map true}}
                       {:id "test"
                        :source-paths[ "src/main/clojure" "src/test/clojure"]
                        :compiler {
                                   :main eginez.huckleberry.test-runner
                                   :output-to "out/test.js"
                                   :target :nodejs
                                   :output-dir "out/test"
                                   :optimizations :none
                                   :parallel-build true
                                   :source-map true}
                        }
                       ]}) 
