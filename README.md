# Huckleberry
A clojurescript library that provides dependency resolution for maven artifacts.
Huckleberry aims to be a jvm-less port of [Pomergranate](https://github.com/cemerick/pomegranate)] and [Aether](https://github.com/sonatype/sonatype-aether).

## Huckleberry supports
* Maven dependencies expressed in lenin style coordinates eg: [commons-logging "1.0"]
* Local repo
* Exclusions

## Huckleberry does not support
* Proxies or Mirrors
* Managed coordinates
* Classpath arithmetic/handling (not needed for js based applications)

## Installation
Huckleberry can be found in clojars. Add it to your leiningen project
```clojure
[org.eginez/huckleberry "0.1.0"]
```

## Usage
The entry function can be used like so
```clojure
(huckleberry/resolve-depedencies :coordinates '[[commons-logging "1.1"]])
```
This will return a tuple with a status flag, a depdency graph and flatten out list of all dependencies
that are required and their location

## License

Copyright (C) 2016 Esteban Ginez

Distributed under the Eclipse Public License, the same as Clojure.


