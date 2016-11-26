(ns eginez.huckleberry.os
  (:require-macros [cljs.core.async.macros :refer [go-loop go]])
  (:refer-clojure :exclude  [type proxy])
  (:require [cljs.nodejs :as nodejs]
            [cljs.core.async :refer [timeout close! put! chan <! >! take!  pipeline alts! poll!] :as async]
            [clojure.set :as set]
            [clojure.string :as str]))


(def path (nodejs/require "path"))
(def fs (nodejs/require "fs"))
(def xml2js (nodejs/require "xml2js"))
(def request (nodejs/require "request"))
;(def dbg (aset request "debug" true))

(def HOME-DIR (-> nodejs/process .-env .-HOME))
(def SEPARATOR (.-sep path))


(defn make-http-request [cout url]
  (.get request #js {:url url :encoding nil}
        (fn [error response body]
          (when (and
                  (not error)
                  (= 200 (.-statusCode response)))
            ;(println (str "Downloaded from " url))
            ;(println (str "Downloaded from " (count body)))
            (put! cout body))))
  cout)

(defn read-file [cout fpath]
  (.readFile fs fpath "utf-8"
             (fn [err data] (when-not err
                              ;(println "Read file " fpath)
                              (put! cout  data))))
  cout)

(defn create-dir-fully [dir-path]
  (if (-> dir-path str/blank? not)
    (try
      (.mkdirSync fs  (str dir-path))
      (catch :default e
        (create-dir-fully (.dirname path dir-path))
        (.mkdirSync fs (str dir-path))))))

(defn create-conditionally [dir-path]
  (try
    (.statSync fs dir-path)
    (catch :default e
      (create-dir-fully dir-path))))

(defn write-file [dep file-path content]
  (do
    (create-conditionally (.dirname path file-path))
    (.writeFileSync fs file-path content)
    dep))


(defn parse-xml [xmlstring]
  "Parses the xml"
  (let [x (chan)]
    (.parseString xml2js xmlstring #(put! x %2))
    (poll! x)))



