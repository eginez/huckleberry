# Huckleberry
A clojurescript library that provides dependency resolution for maven artifacts.
Huckleberry aims to be a jvm-less port of [Pomergranate](https://github.com/cemerick/pomegranate)] and [Aether](https://github.com/sonatype/sonatype-aether).

## Huckleberry supports
* Maven dependencies expressed in lenin style coordinates eg: [commons-logging "1.0"]
* 
* Exclusions

## Huckleberry does not support
* Classpath arithmetic/handling (not needed for js based applications)
* Proxies
* Mirrors
* Managed coordinates

## Installation



## Usage

```bash
lein deps
lein run
```

## License

Copyright (C) 2012 FIXME

Distributed under the Eclipse Public License, the same as Clojure.


