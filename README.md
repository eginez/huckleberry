# Huckleberry [![Clojars Project](https://img.shields.io/clojars/v/org.clojars.eginez/huckleberry.svg)](https://clojars.org/org.clojars.eginez/huckleberry)
A clojurescript library that provides dependency resolution for maven artifacts.
Huckleberry aims to be a jvm-less replacement for [Pomergranate](https://github.com/cemerick/pomegranate) and [Aether](https://github.com/sonatype/sonatype-aether), where possible. 

## Huckleberry supports
* Maven dependencies expressed in lein style coordinates eg: [commons-logging "1.0"]
* Local repo
* Exclusions
* Resolving transient dependencies via the parent or using versions interpolated from the properties in the POM file where required.

## Huckleberry does not support
* Proxies or Mirrors
* Managed coordinates
* Classpath arithmetic/handling

## Installation
Huckleberry can be found in clojars. Add it to your leiningen project
```clojure
[org.clojars.eginez/huckleberry "0.2.0"]
```

## Usage
The entry function can be used like so
```clojure
(huckleberry/resolve-depedencies :coordinates '[[commons-logging "1.1" :retrieve false]])
```
This will return a channel which will push a list with [status depedency-graph flatten-depdency-list]

```clojure
(huckleberry/resolve-depedencies :coordinates '[[commons-logging "1.1" :retrieve true]])
```
Will return channel that will output the status of each of the files that need to be downloaded

For more examples on how to use the library look in the test [directory](https://github.com/eginez/huckleberry/blob/master/src/test/clojure/eginez/huckleberry/core_test.cljs)

## Running the tests

Run `lein deps` followed by `lein doo node test`.

## License

Copyright (C) 2016 Esteban Ginez

Distributed under the Eclipse Public License, the same as Clojure.


