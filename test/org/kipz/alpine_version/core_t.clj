(ns org.kipz.alpine-version.core-t
  (:require [clojure.test :refer [deftest is]]
            [org.kipz.alpine-version.core :refer [parse-version
                                               compare-versions
                                               in-range?]]
            [clojure.edn :as edn]))

(deftest version-parsing
  (is (= [["23_foo" ">  4_beta"]]
         (remove
          #(parse-version (first %))
          (edn/read-string (slurp "test/ranges.edn"))))))

(deftest comparing-versions
  (is (false? (compare-versions "1.3.4_alpha" "1.2.3"))))

(deftest version-ranges
  (is (= [["23_foo" ">  4_beta"]]
         (remove
          #(in-range? (first %) (second %))
          (edn/read-string (slurp "test/ranges.edn"))))))
