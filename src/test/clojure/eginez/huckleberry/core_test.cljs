(ns eginez.huckleberry.core-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.test :refer-macros [async deftest is testing]]
            [cljs.core.async :refer [put! take! chan <! >!] :as async]
            [cljs.pprint :as pp]
            [eginez.huckleberry.core :as maven]))

;com/cognitect/transit-java/0.8.313
(def test-dep {:group "commons-logging" :artifact "commons-logging" :version "1.1"})
(def test-dep-exclusions {:group "commons-logging" :artifact "commons-logging" :version "1.1"
                :exclusions [{:group "avalon-framework" :artifact "avalon-framework" :version "4.1.3"}]})
;(def test-dep {:group "com.cognitect" :artifact "transit-java" :version "0.8.313"})
(def test-dep2 {:group "cljs-bach" :artifact "cljs-bach" :version "0.2.0"})
(def test-dep3 {:group "reagent" :artifact "reagent" :version "0.6.0-alpha2"})
(def test-dep4 {:group "junit" :artifact "junit" :version "4.12"})
(def test-dep5 {:group "org.clojure" :artifact "clojure" :version "1.8.0"})
(def test-dep6 {:group "commons-logging" :artifact "commons-logging" :version "1.1"
                :exclusions [{:group "avalon-framework" :artifact "avalon-framework" :version "4.1.3"}]})

(def test-url (maven/create-urls-for-dependency (:maven-central maven/repos) test-dep))

(deftest create-url
  (let [urls (maven/create-urls-for-dependency (:local maven/repos) test-dep)]
    (assert (-> urls first coll? not))))

;(deftest create-url-repos
;  (let [urls (maven/create-urls-for-dependency (:clojars repos) test-dep)]
;    (assert (-> urls first coll?))))

(deftest test-resolve-single
  (async done
    (go
      (let [[status d locations] (<! (maven/resolve test-dep :repositories (vals maven/repos)))]
        (assert (= 5 (-> d keys count)))
        (assert (= 5 (-> locations count)))
        ;(is (= '#{{:group "commons-logging",
        ;        :artifact "commons-logging",
        ;        :version "1.1"}
        ;       {:group "log4j", :artifact "log4j", :version "1.2.12"}
        ;       {:group "logkit", :artifact "logkit", :version "1.0.1"}
        ;       {:group "avalon-framework",
        ;        :artifact "avalon-framework",
        ;        :version "4.1.3"}
        ;       {:group "javax.servlet", :artifact "servlet-api", :version "2.3"}}
        ;    d))
        (done)))))

(deftest test-resolve-all-single
  (async done
    (go
      (let [[status d l] (<! (maven/resolve-all [test-dep] :repositories (vals maven/repos)))]
        (is (= 2 (count d)))
        (is (= test-dep (first d)))
        (is (= 5 (-> d second keys count)))
        (is (= 5 (-> l count)))
        (done)))))

(deftest test-resolve-all-single-with-exclusion
  (async done
    (go
      (let [[status d l] (<! (maven/resolve-all [test-dep-exclusions] :repositories (vals maven/repos)))]
        ;(pp/pprint d)
        (is (= 2 (count d)))
        (is (= test-dep-exclusions (first d)))
        (is (= 4 (-> d second keys count)))
        (done)))))

(deftest test-resolve-all-single2
  (async done
    (go
      (let [[status d l] (<! (maven/resolve-all [test-dep3 test-dep-exclusions] :repositories (vals maven/repos)))]
        (is (= 4 (count d)))
        (done)))))

(deftest test-resolve-coordinates
  (async done
    (go
      (let [[status d l] (<! (maven/resolve-dependencies :coordinates '[[commons-logging "1.1"]]
                                                        :retrieve false
                                                        :local-repo nil))]
        (is (= 2 (count d)))
        (is (= 5 (-> d second keys count)))
        (done)
        ))))


(deftest test-resolve-dep2
  (let [deps '[[commons-logging "1.1"]
               [log4j "1.2.15" :exclusions [[javax.mail/mail :extension "jar"]
                                            [javax.jms/jms :classifier "*"]
                                            com.sun.jdmk/jmxtools
                                            com.sun.jmx/jmxri]]]
        ]
    (async done
      (go
        (let [[status dp list] (<! (maven/resolve-dependencies :coordinates deps
                                                      :retrieve false
                                                      :local-repo nil))]
          (is (= 4 (count dp)))
          (is (= 5 (-> dp second keys count)))
          (is (= 1 (count (last dp))))
          (done)
          )))))

;(deftest test-resolve-coordinates-download
;  (async done
;    (go
;      (let [[status d l] (<! (maven/resolve-dependencies :coordinates '[[commons-logging "1.1"]]
;                                                         :retrieve true
;                                                         :local-repo nil))]
;        (is (= 2 (count d)))
;        (is (= 5 (-> d second keys count)))
;        (done)
;        ))))
