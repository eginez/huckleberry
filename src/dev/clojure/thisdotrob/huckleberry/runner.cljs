(ns ^:figwheel-always thisdotrob.huckleberry.runner
  (:require [cljs.nodejs :as nodejs]))

(nodejs/enable-util-print!)

(defn -main []
  (println "Huckleberry runner started!"))

(set! cljs.core/*main-cli-fn* -main)
