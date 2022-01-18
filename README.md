# alpine-version-clj

[![Clojars Project](https://img.shields.io/clojars/v/org.kipz/alpine-version-clj.svg)](https://clojars.org/org.kipz/alpine-version-clj)

Parse alpine-version (and gentoo) scheme as per:

* https://wiki.alpinelinux.org/wiki/Package_policies
* https://projects.gentoo.org/pms/8/pms.html#x1-250003.2
* https://gitlab.alpinelinux.org/alpine/apk-tools/-/blob/master/src/version.c

Thanks to https://github.com/knqyf263/go-apk-version from which I've pulled some test data.

```clj
[org.kipz/alpine-version-clj "<some version>"]
```

## Usage from Clojure

### Parse a version

```clj
(:require [org.kipz.alpine-version.core :refer [parse-version]])
;; returns nil if can't parse
(parse-version "1.0.2.3_pre1_alpha1_rc")
;=>
{:numbers ("1" "0" "2" "3"),
 :revision 0,
 :letter "",
 :suffixes ({:suffix "pre", :number 1} {:suffix "alpha", :number 1} {:suffix "rc"})}
```

### Compare two versions

```clj
(:require [org.kipz.alpine-version.core :refer [compare-versions]])
(compare-versions "2.1a_alpha" "2.1a_pre")
; => true first arg is lower/before second
```

### Sorting

As per normal Clojure awesomeness, we can use it as a normal comparator

```clj
(sort compare-versions ["1.2.3-r1" "1.2.3" "0.1.1" "1.3.4_alpha"] )
; => ("0.1.1" "1.2.3" "1.2.3-r1" "1.3.4_alpha")
```

### Range checking

Easily check if a version is in a particular range (two ranges are supported optionally separated by an &)

The following operators are allowed: `< > <= >= =`

```clj
(:require [org.kipz.alpine-version.core :refer [in-range?]])
(in-range? "1.2.3-r1" ">  1.2.3")
; => true
(in-range? "1.2.3-r1" ">  1.2.3 & < 1.3.4_alpha" )
; => true
```
