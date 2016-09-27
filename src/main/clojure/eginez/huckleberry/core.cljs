(ns eginez.huckleberry.core
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
            ;(println (str "Downloaded from " url))
            (put! cout body))))
  cout)

(defn read-file [cout fpath]
  (.readFile fs fpath "utf-8"
             (fn [err data] (when-not err
                              ;(println "Read file " fpath)
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
                    (map #(conj [] url %))
                    ))]
    (read-url-chan c url)))

(defn extract-dependencies [urls]
  (map create-pipeline urls))

(defn artifact->coordinate [artifact]
  (str (:group artifact) "/" (:artifact artifact)))


(defn resolve [dep &{:keys [repositories local-repo]} ]
  (go-loop [next dep
            to-do #{}
            done {}
            locations #{}
            exclusions (into #{} (:exclusions dep))
            status true]
           (if next
             (do
               ;(println next)
               (let [no-excl (dissoc next :exclusions)
                     url-set (create-urls-for-dependency repositories no-excl)
                     urls (map first url-set)
                     repo-reqs (extract-dependencies urls)
                     tout (timeout 5000)
                     repo-reqs (conj repo-reqs tout)
                     [[url deps] ch] (alts! repo-reqs)
                     to-kill (filter #(not (identical? ch %)) repo-reqs)
                     real-deps (set/difference deps exclusions) ;?
                     new-dep (set/union to-do real-deps)
                     new-locations (set/union locations (conj #{} (assoc no-excl :url url)))
                     done (into done {no-excl real-deps})]
                 (if (not (identical? tout ch))
                   (do
                     (map close! to-kill)
                     (recur (first new-dep) (rest new-dep) done new-locations exclusions true))
                   (recur nil nil next [] [] false))))
             [status done locations])))

(defn resolve-all [all-deps & opts]
  (go
    (loop [deps all-deps
           dg []
           locations #{}
           r-status true]
      (if (and r-status (-> deps empty? not))
        (let [to-resolve (first deps)
              [status res-dep new-locations] (<! (apply resolve to-resolve opts))
              new-dg (conj dg to-resolve res-dep)]
          (recur (rest deps) new-dg (set/union locations new-locations) status))
        [r-status dg locations]))))

(defn- group
  [group-artifact]
  (or (namespace group-artifact) (name group-artifact)))


(defn- exclusion
  [[group-artifact & {:as opts}]]
  {:group (group group-artifact)
   :artifact (name group-artifact)
   :classifier (:classifier opts "*")
   :extension (:extension opts "*")})

(defn- normalize-exclusion-spec [spec]
  (if (symbol? spec)
    [spec]
    spec))

(defn- dependency
  [[group-artifact version & {:keys [scope optional exclusions]
                              :as opts
                              :or {scope "compile"
                                   optional false}}
    :as dep-spec]]
  {:group (group group-artifact)
   :artifact (name group-artifact)
   :version version
   ;:scope scope
   ;:optional optional
   :exclusions (map (comp exclusion normalize-exclusion-spec) exclusions)})

(defn retrieve [dep in-repo]
  (let [url (:url dep)
        jar-url (str/replace url #"pom" "jar")]
  (println "Downloading " jar-url " to " in-repo)))

(defn retrieve-deps [[status dg dep-list] local-repo]
  (let [remote-deps (filter #(-> % :url is-url-local? not) dep-list)]
    (map #(retrieve % local-repo) remote-deps)))

(defn resolve-dependencies
  [& {:keys [repositories coordinates managed-coordinates files retrieve local-repo
             transfer-listener offline? proxy mirrors repository-session-fn]
      :or {retrieve true}}]
  (go
    (let [repos (or repositories (vals repos))
          deps-map (map dependency coordinates)
          deps-chan (<! (resolve-all deps-map :repositories repos
                                     :local-repo local-repo))]
      (if retrieve (retrieve-deps deps-chan local-repo) deps-chan))))


