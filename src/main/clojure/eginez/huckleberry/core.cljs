(ns eginez.huckleberry.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:refer-clojure :exclude  [type proxy])
  (:require [cljs.nodejs :as nodejs]
            [cljs.core.async :refer [timeout close! put! chan <! >! take!  pipeline alts! poll!] :as async]
            [clojure.set :as set]
            [clojure.string :as str]))

(def path (nodejs/require "path"))
(def fs (nodejs/require "fs"))
(def xml2js (nodejs/require "xml2js"))
(def request (nodejs/require "request"))
(def repos {:clojars "https://clojars.org/repo"
            :local (.join path (-> nodejs/process .-env .-HOME) ".m2" "repository")
            :maven-central "https://repo1.maven.org/maven2"})

(defn is-url-local? [url]
  (not (str/starts-with? url "http")))

(defn create-remote-url-for-depedency [repo {group :group artifact :artifact version :version}]
  (let [sep (if (is-url-local? repo) "/" (.-sep path))
        g (str/replace group #"\." sep)
        art (str/join "-" [artifact version])
        art-url (str/join sep [repo g artifact version art])
        ext ["pom" "jar"]]
    (map #(str/join "." [art-url %]) ext)))


(defn create-urls-for-dependency [repos d]
  (if (coll? repos)
    (map #(create-remote-url-for-depedency % d) repos)
    (create-remote-url-for-depedency repos d)))

(defn make-http-request [cout url]
  (.get request #js {:url url}
        (fn [error response body]
          (when (and
                  (not error)
                  (= 200 (.-statusCode response)))
            (println (str "Downloaded from " url))
            (put! cout body))))
  cout)

(defn read-file [cout fpath]
  (.readFile fs fpath "utf-8"
             (fn [err data] (when-not err
                              (println "Read file " fpath)
                              (put! cout  data))))
  cout)

(defn read-url-chan [cout url]
  (if (is-url-local? url)
    (read-file cout url)
    (make-http-request cout url)))

(defn parse-xml [xmlstring]
  "Parses the xml"
  (let [x (chan)]
    (.parseString xml2js xmlstring #(put! x %2))
    (poll! x)))

(defn mvndep->dep [x]
  (let [g (first (:groupId x))
        a (first (:artifactId x))
        v (first (:version x))
        m {:group g :artifact a :version v}]
    m))

(defn clean-deps [x]
  (let [ y (remove #(or
                     (= "test" (first (:scope %)))
                     (= "true" (first (:optional %))))
                   x)]
    y))


(defn create-pipeline [url]
  (let [c (chan 1 (comp
                    (map parse-xml)
                    (map #(js->clj % :keywordize-keys true))
                    (map #(get-in % [:project :dependencies 0 :dependency]))
                    (map clean-deps)
                    (map #(map mvndep->dep %))
                    (map #(into #{} %))
                    ))]
    (read-url-chan c url)))

(defn extract-dependencies [urls]
  (map create-pipeline urls))

(defn resolve [dep]
  (let [x (chan)]
    (go
      (loop [next dep
             to-do #{}
             done #{}
             status true]
        (if next
          (do
            (println "Looking for dependencies for " next)
            (let [url-set (create-urls-for-dependency (vals repos) next)
                  urls (map first url-set)
                  repo-reqs (extract-dependencies urls)
                  tout (timeout 2000)
                  repo-reqs (conj repo-reqs tout)
                  [deps ch] (alts! repo-reqs)
                  to-kill (filter #(not (identical? ch %)) repo-reqs)
                  not-done (set/difference deps done)
                  new-dep (set/union to-do not-done)
                  done (conj done next) ]
              (if (not (identical? tout ch))
                (do
                  (map close! to-kill)
                  (recur (first new-dep) (rest new-dep) done true))
                (recur nil nil next false))))
          (put! x [status done])))
      (close! x))
    x))


(defn resolve-dependencies [coordinates repos]
  (println coordinates)
  )

(defn retrieve-dependencies [deps-graph]
  nil)
