(ns org.kipz.alpine-version.core
  (:require [clojure.string :as str]))

(def ^:private version-spec
  #"^(\d+)(?:\.(\d+))*([a-z]?)((_alpha|_beta|_pre|_rc|_p)(\d*))*?(?:-r(\d+))?$")

(defn parse-version
  "Return a datastructure (see Readme) or nil"
  [version]
  (when (string? version)
    (when-let [[_ primary _ letter _ _ _ revision] (re-matches version-spec version)]
      ;; java doesn't capture repeating groups - but we know it parsed ok
      (let [revision (if (empty? revision)
                       0
                       (Integer/parseInt revision))
            dot-prefixed (map second (re-seq #"(?:\.(\d+))" version))
            suffixes (map
                      (fn [s]
                        {:suffix (second s)
                         :number (if (not-empty (last s))
                                   (Integer/parseInt (last s))
                                   0)})
                      (re-seq #"_(alpha|beta|pre|rc|p)(\d*)" version))]
        (cond->
         {:numbers (concat [primary] dot-prefixed)
          :revision revision}

          letter
          (assoc :letter letter)

          (not-empty suffixes)
          (assoc :suffixes suffixes))))))

(defn- leading-zero?
  [some-str]
  (and
   (string? some-str)
   (str/starts-with? some-str "0")))

(defn- without-trailing-zeros
  [some-str]
  (->> some-str
       reverse
       (drop-while #(=  \0 %))
       reverse
       (apply str)))

(defn- compare-numbers
  "Alg: 3.3"
  [ans bns]
  (let [ans (rest (:numbers ans))
        bns (rest (:numbers bns))]
    (boolean

     (loop [ans ans bns bns]
       (let [an (first ans)
             bn (first bns)]
         (when (and an bn)
           (if (or (leading-zero? an)
                   (leading-zero? bn))
             (let [an- (without-trailing-zeros an)
                   bn- (without-trailing-zeros bn)]
               (or
                (< (compare an- bn-) 0)
                (and (= an- bn-)
                     (recur (rest ans) (rest bns)))))
             (or (< (Integer/parseInt an)
                    (Integer/parseInt bn))
                 (and
                  (= an bn)
                  (recur (rest ans) (rest bns)))))))))))

(def ^:private suffix-priority {"alpha" 1 "beta" 2 "pre" 3 "rc" 4 "p" 5})

(defn compare-suffix
  "Alg: 3.6"
  [as bs]
  (if (= (:suffix as) (:suffix bs))
    (< (:number as) (:number bs))
    (< (suffix-priority (:suffix as))
       (suffix-priority (:suffix bs)))))
(defn- compare-suffixes
  "Alg: 3.5"
  [a b]
  (boolean
   (let [as (:suffixes a) bs (:suffixes b)]
     (or
      (loop [as as bs bs]
        (let [as- (first as)
              bs- (first bs)]
          (when (and as- bs-)
            (or
             (compare-suffix as- bs-)
             (recur (rest as) (rest bs))))))
      (or (and
           (< (count as) (count bs))
           (= "p" (:suffix (nth bs (count as)))))
          (and
           (> (count as) (count bs))
           (not= "p" (:suffix (nth as (count bs))))))))))

(defn- normalize
  "Convert to numberical as needed"
  [v1 v2 c]
  (let [r (c v1 v2)]
    (cond
      (number? r) r
      (true? r) -1
      (c v2 v1) 1
      :else 0)))

(defn- compare-primary
  [v1 v2]
  (< (Integer/parseInt (first (:numbers v1)))
     (Integer/parseInt (first (:numbers v2)))))

(defn- compare-in-order
  "Return first non-zero result"
  [v1 v2 & fns]
  (boolean
   (some->>
    fns
    (map (partial normalize v1 v2))
    (drop-while zero?)
    first
    (> 0))))

(defn- compare-number-counts
  [v1 v2]
  (< (count (str (rest (:numbers v1))))
     (count (str (rest (:numbers v2))))))

(defn compare-versions
  "Compare to debian package version strings. Returns true if v1 is before/lower than v2"
  [v1s v2s]
  (boolean
   (let [v1 (parse-version v1s)
         v2 (parse-version v2s)]
     (or
      (when (and v1 v2)
       ;; alg: 3.2
        (compare-in-order
         v1 v2
         compare-primary
         compare-numbers
         compare-number-counts
         #(compare (:letter %1) (:letter %2))
         compare-suffixes
         #(< (count (:suffixes %1))
             (count (:suffixes %2)))
         #(< (:revision %1) (:revision %2))))

      ;; fall back to string comparison?
      (< (compare v1s v2s) 0)))))

(def ^:private range-operators #"(\>=|\<=|\<|\>|=)")

(defn- split-ranges
  "Pre-clean ranges for easier version matching"
  [range-str]
  (str/split
   (str/replace
    (str/trim
     (str/replace
      range-str
      #"[\<\>=,&]"
      " "))
    #"\s+"
    " ")
   #" "))


(defn- compare-to-range
  [version operator range]
  (boolean
   (cond
     (= "=" operator)
     (= version range)

     (= "<" operator)
     (compare-versions version range)

     (= ">" operator)
     (compare-versions range version)

     (= "<=" operator)
     (or (= version range)
         (compare-versions version range))

     (= ">=" operator)
     (or (= version range)
         (compare-versions range version)))))

(defn in-range?
  "Is version in range string (e.g. < 12.23 & > 14.1~foo)"
  [version range]
  (boolean
   (when (and
          (string? version)
          (string? range))
     (let [[range-version1 range-version2] (split-ranges range)
           [operator1 operator2] (map second (re-seq range-operators range))]
       (when (and range-version1 range-version1)
         (and
          (compare-to-range version operator1 range-version1)
          (or
           (not (and range-version2 operator2))
           (compare-to-range version operator2 range-version2))))))))
