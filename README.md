# Higher Kinded Type machinery for Java

[![Gitter](https://badges.gitter.im/derive4j/hkt.svg)](https://gitter.im/derive4j/hkt)
[![Maven Central](https://img.shields.io/maven-central/v/org.derive4j.hkt/hkt.svg)][search.maven]
[![Travis](https://travis-ci.org/derive4j/hkt.svg?branch=master)](https://travis-ci.org/derive4j/hkt)

This project aims at providing type-safety for the higher kinded type encoding demonstrated in https://github.com/DanielGronau/highj via a JSR269 annotation processor.

Initial design was discussed in https://github.com/derive4j/hkt/issues/1

# Usage

## Maven
```xml
<dependency>
  <groupId>org.derive4j.hkt</groupId>
  <artifactId>hkt</artifactId>
  <version>0.2</version>
</dependency>
```
[search.maven]: http://search.maven.org/#search|ga|1|org.derive4j.hkt

## Gradle
```
compile(group: 'org.derive4j.hkt', name: 'hkt', version: '0.2', ext: 'jar')
