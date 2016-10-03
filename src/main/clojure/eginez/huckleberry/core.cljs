(ns eginez.huckleberry.core
  (:require-macros [cljs.core.async.macros :refer [go-loop go]])
  (:refer-clojure :exclude  [type proxy])
  (:require [cljs.nodejs :as nodejs]
            [cljs.core.async :refer [timeout close! put! chan <! >! take!  pipeline alts! poll!] :as async]
            [clojure.set :as set]
            [eginez.huckleberry.os :as os]
            [clojure.string :as str]))

(def repos {:clojars "https://clojars.org/repo"
            :local (str/join os/SEPARATOR [os/HOME-DIR  ".m2" "repository"])
            :maven-central "https://repo1.maven.org/maven2"})

(defn is-url-local? [url]
  (not (str/starts-with? url "http")))

(defn create-remote-url-for-depedency [repo {group :group artifact :artifact version :version}]
  (let [sep (if (is-url-local? repo) "/" os/SEPARATOR)
        g (str/replace group #"\." sep)
        art (str/join "-" [artifact version])
        art-url (str/join sep [repo g artifact version art])
        ext ["pom" "jar"]]
    [repo (map #(str/join "." [art-url %]) ext)]))


(defn create-urls-for-dependency [repos d]
  (if (coll? repos)
    (map #(create-remote-url-for-depedency % d) repos)
    (create-remote-url-for-depedency repos d)))




(defn read-url-chan [cout url]
  (if (is-url-local? url)
    (os/read-file cout url)
    (os/make-http-request cout url)))


(defn mvndep->dep [x]
  (let [g (first (:groupId x))
        a (first (:artifactId x))
        v (first (:version x))
        m {:group g :artifact a :version v}]
    m))

(defn dep->coordinate [dep]
  (str (:group dep) "/" (:artifact dep) " " (:version dep)))

(defn clean-deps [x]
  (let [ y (remove #(or
                     (= "test" (first (:scope %)))
                     (= "true" (first (:optional %))))
                   x)]
    y))


(defn read-dependency-pipeline [url-set]
  "Creates a read depedency pipeline that extracts maven dependecy from a url-set
  A url set is looks like [repo '(jar-url pom-url)]"
  (let [repo-url (first url-set)
        pom-url (-> url-set second first)
        c (chan 1 (comp
                    (map os/parse-xml)
                    (map #(js->clj % :keywordize-keys true))
                    (map #(get-in % [:project :dependencies 0 :dependency]))
                    (map clean-deps)
                    (map #(map mvndep->dep %))
                    (map #(into #{} %))
                    (map #(conj [] repo-url %))
                    ))]
    (read-url-chan c pom-url)))

(defn extract-dependencies [url-set]
  (map read-dependency-pipeline url-set))

(defn resolve [dep &{:keys [repositories local-repo]} ]
  (go-loop [next dep
            to-do #{}
            done {}
            locations #{}
            exclusions (:exclusions dep)
            status true]
           (if next
             (do
               ;(println next)
               (let [no-excl (dissoc next :exclusions)
                     url-set (create-urls-for-dependency repositories no-excl)
                     urls (map #(-> % second first) url-set)
                     repo-reqs (extract-dependencies url-set)
                     tout (timeout 5000)
                     repo-reqs (conj repo-reqs tout)
                     [[url deps] ch] (alts! repo-reqs)
                     to-kill (filter #(not (identical? ch %)) repo-reqs)
                     real-deps (filter (fn [x]
                                           (empty? (filter #(and
                                                     (= (:group %) (:group x))
                                                     (= (:artifact %) (:artifact x))) exclusions)))
                                 deps)

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


(defn download-and-save-pipeline [[download-from save-to] dep]
  (let [c (chan 1024 (comp
                       (map #(do (println "Downloading " download-from) %))
                       (map #(os/write-file save-to %))))]
    (os/make-http-request c download-from)))

(defn retrieve [dep in-repo]
  (let [ repo-url (:url dep)
        urls (create-urls-for-dependency repo-url dep)
        save-to-locations (create-urls-for-dependency in-repo dep)
        urls-to-proc (map vector (second urls) (second save-to-locations))]
    (map #(download-and-save-pipeline % dep) urls-to-proc)))

(defn retrieve-dependencies [[status dg dep-list] local-repo offline?]
  (let [remote-deps (filter #(-> % :url is-url-local? not) dep-list)]
    (if (and offline? (nil? local-repo))
      nil
      (if (empty? remote-deps)
        [(go true)]
        (flatten (map #(retrieve % local-repo) remote-deps))))))

(defn resolve-dependencies
  [& {:keys [repositories coordinates retrieve local-repo
             proxy mirrors managed-coordinates]
      :or {retrieve true}}]
  (go
    (let [repos (or repositories (vals repos))
          offline? false
          deps-map (map dependency coordinates)
          deps-chan (<! (resolve-all deps-map :repositories repos
                                     :local-repo local-repo))]
      (if retrieve
        (<!(async/map vector (retrieve-dependencies deps-chan local-repo offline?)))
        deps-chan))))


