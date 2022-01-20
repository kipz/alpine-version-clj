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

(def ranges [["16.13.2-r0" "< 8.11.4-r0" false]
             ["16.13.2-r0" "< 8.11.3-r0" false]
             ["16.13.2-r0" "< 8.9.3-r0" false]
             ["16.13.2-r0" "< 8.11.3-r0" false]
             ["2.4.1-r0" "< 2.4.3-r0" true]
             ["16.13.2-r0" "< 8.11.0-r0" false]
             ["2.4.1-r0" "< 2.4.3-r0" true]
             ["16.13.2-r0" "<8.9.3-r0" false]
             ["2.4.1-r0" "<2.4.3-r0" true]
             ["16.13.2-r0" "<8.11.3-r0" false]
             ["16.13.2-r0" "<6.11.5-r0" false]
             ["16.13.2-r0" "<6.11.1-r0" false]
             ["2.4.1-r0" "<2.4.3-r0" true]
             ["1.2.2-r7" "<1.2.2_pre2-r0" false]])

(deftest real-version-ranges
  (is (=
       (map last ranges)
       (map
        #(in-range? (first %) (second %))
        ranges))))
