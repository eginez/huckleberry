(ns eginez.huckleberry.core-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.test :refer-macros [async deftest is testing]]
            [cljs.core.async :refer [put! take! chan <! >!] :as async]
            [clojure.string :as strg]
            [cljs.pprint :as pp]
            [eginez.huckleberry.core :as huckleberry]))

(def test-dep {:group "commons-logging" :artifact "commons-logging" :version "1.1"})
(def test-dep-exclusions {:group "commons-logging" :artifact "commons-logging" :version "1.1"
                :exclusions [{:group "avalon-framework" :artifact "avalon-framework" :version "4.1.3"}]})
(def test-dep2 {:group "cljs-bach" :artifact "cljs-bach" :version "0.2.0"})
(def test-dep3 {:group "reagent" :artifact "reagent" :version "0.6.0-alpha2"})
(def test-dep4 {:group "junit" :artifact "junit" :version "4.12"})
(def test-dep5 {:group "org.clojure" :artifact "clojure" :version "1.8.0"})
(def test-dep6 {:group "commons-logging" :artifact "commons-logging" :version "1.1"
                :exclusions [{:group "avalon-framework" :artifact "avalon-framework" :version "4.1.3"}]})
(def test-dep7 {:group "org.clojure" :artifact "core.specs.alpha" :version "0.1.24"})
(def test-dep8 {:group "org.apache.httpcomponents" :artifact "httpasyncclient" :version "4.1.3"})

(def test-url (huckleberry/create-urls-for-dependency (:maven-central huckleberry/default-repos) test-dep))

(deftest create-url
  (let [urls (huckleberry/create-urls-for-dependency (:local huckleberry/default-repos) test-dep)]
    (assert (-> urls first coll? not))))

(deftest test-resolve-single
  (async done
    (go
      (let [[status d locations] (<! (huckleberry/resolve test-dep :repositories (vals huckleberry/default-repos)))]
        (assert (true? status))
        (assert (= 5 (-> d keys count)))
        (assert (= 5 (-> locations count)))
        (done)))))

(deftest test-resolve-single-with-variable-interpolation
  (async done
    (go
      (let [[status d locations] (<! (huckleberry/resolve test-dep7 :repositories (vals huckleberry/default-repos)))]
        (assert (true? status))
        (assert (= 7 (-> d keys count)))
        (assert (= 7 (-> locations count)))
        (done)))))

(deftest test-resolve-single-with-parent
  (async done
    (go
      (let [[status d locations] (<! (huckleberry/resolve test-dep8 :repositories (vals huckleberry/default-repos)))]
        (println (-> d keys count))
        (println (-> locations count))
        (assert (true? status))
        (assert (= 4 (-> d keys count)))
        (assert (= 4 (-> locations count)))
        (done)))))

(deftest test-resolve-all-single
  (async done
    (go
      (let [[status d l] (<! (huckleberry/resolve-all [test-dep] :repositories (vals huckleberry/default-repos)))]
        (is (= 2 (count d)))
        (is (= test-dep (first d)))
        (is (= 5 (-> d second keys count)))
        (is (= 5 (-> l count)))
        (done)))))

(deftest test-resolve-all-single-with-exclusion
  (async done
    (go
      (let [[status d l] (<! (huckleberry/resolve-all [test-dep-exclusions] :repositories (vals huckleberry/default-repos)))]
        (is (= 2 (count d)))
        (is (= test-dep-exclusions (first d)))
        (is (= 4 (-> d second keys count)))
        (done)))))

(deftest test-resolve-all-single2
  (async done
    (go
      (let [[status d l] (<! (huckleberry/resolve-all [test-dep3 test-dep-exclusions] :repositories (vals huckleberry/default-repos)))]
        (is (= 4 (count d)))
        (done)))))

(deftest test-resolve-coordinates
  (async done
    (go
      (let [[status d l] (<! (huckleberry/resolve-dependencies :coordinates '[[commons-logging "1.1"]]
                                                               :retrieve false
                                                               :local-repo nil))]
        (is (= 2 (count d)))
        (is (= 5 (-> d second keys count)))
        (print "List of dependencies:")
        (pp/pprint l)
        (print "Dependency graph:")
        (pp/pprint d)
        (done)
        ))))


(deftest test-resolve-dep2
  (let [deps '[[commons-logging "1.1"]
               [log4j "1.2.15" :exclusions [[javax.mail/mail]
                                            [javax.jms/jms]
                                            com.sun.jdmk/jmxtools
                                            com.sun.jmx/jmxri]]]
        ]
    (async done
      (go
        (let [[status dp list] (<! (huckleberry/resolve-dependencies :coordinates deps
                                                                     :retrieve false
                                                                     :local-repo nil))]
          (is (= 4 (count dp)))
          (is (= 5 (-> dp second keys count)))
          (is (= 1 (count (last dp))))
          (done)
          )))))

(deftest test-retrieve
  (async done
    (go
      (let [dp  (<! (huckleberry/retrieve
                      {:group "log4j", :artifact "log4j", :version "1.2.12" :url "https://repo1.maven.org/maven2"} "/tmp/huckleberry/test"))]
        (println dp)
        (done)))))

(deftest retrieve-dep-list
  (async done
    (go
      (let [dp (<! (huckleberry/retrieve-dependencies
                     [nil nil [
                               {:group "commons-logging" :artifact "commons-logging" :version "1.1" :url "https://repo1.maven.org/maven2"}
                               {:group "log4j", :artifact "log4j", :version "1.2.12" :url "https://repo1.maven.org/maven2"}
                               ]]
                      "/tmp/huckleberry/test" false))]
        (assert (= (count dp) 2))
        (assert (strg/includes? (:url (first dp)) "/tmp/huckleberry"))
        (done)))))


(deftest retrieve-dep-list-with-local
  (async done
    (go
      (let [dp (<! (huckleberry/retrieve-dependencies
                     [nil nil [
                               {:group "commons-logging" :artifact "commons-logging" :version "1.1" :url "https://repo1.maven.org/maven2"}
                               {:group "log4j", :artifact "log4j", :version "1.2.12" :url "/dev/null"}
                               ]]
                     "/tmp/huckleberry/test" false))]
        (assert (= (count dp) 2))
        (println dp)
        (done)))))

(deftest test-resolve-coordinates-download
  (async done
    (go
      (let [ r (<! (huckleberry/resolve-dependencies :coordinates '[[commons-logging "1.1"]]
                                                     :retrieve true
                                                     :local-repo "/tmp/huckleberry/test"))]
        (assert (not (empty? r)))
        ;(println r)
        (done)
        ))))

(deftest test-resolve-coordinates-no-download
  (async done
    (go
      (let [ r (<! (huckleberry/resolve-dependencies :coordinates '[[commons-logging "1.1"]]
                                                     :retrieve false
                                                     :local-repo "/tmp/huckleberry/test"))]
        (assert (= 3 (count r)))
        (done)
        ))))

(deftest test-dp-path
  (assert (not (empty? (huckleberry/dep->path {:group "org.clojure", :artifact "core.cache", :version "0.6.5", :url "tmp/hb/"})))))
