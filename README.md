# Higher Kinded Type machinery for Java

[![Gitter](https://badges.gitter.im/derive4j/hkt.svg)](https://gitter.im/derive4j/hkt)
[![Maven Central](https://img.shields.io/maven-central/v/org.derive4j.hkt/hkt.svg)][search.maven]
[![Travis](https://travis-ci.org/derive4j/hkt.svg?branch=master)](https://travis-ci.org/derive4j/hkt)

This project provides type-safety for the higher kinded type encoding demonstrated in https://github.com/highj/highj via a JSR269 annotation processor.

For some theorical explanation of the encoding you may refer to the [Lightweight higher-kinded polymorphism](https://www.cl.cam.ac.uk/~jdy22/papers/lightweight-higher-kinded-polymorphism.pdf) paper.

# Usage

## Choose your HK encoding:

the two basic possibilities are:
```java
class HkTest<A> implements __<HkTest<?>, A> {

}
```
and
```java
class HkTest<A> implements __<HkTest.w, A> {
  enum w { // could be any name, also could be a static nested class.
  }
}
```
We say that `__<HkTest.w, A>` is the HK encoding of HkTest<A>.

## Obligatory Monad example:
The higher kinded polymorpism allow us the express things that are normaly impossible in Java. Eg.:
```java
public interface Monad<m> {
  <A> __<m, A> pure(A a);

  <A, B> __<m, B> bind(__<m, A> ma, Function<A, __<m, B>> f);

  default <A, B> __<m, B> map(__<m, A> ma, Function<A, B> f) {
    return bind(ma, f.andThen(this::pure));
  }
}
```

### Aliases interfaces
You may want to create aliases of derive4j hkt `__*` interfaces that better suit you naming preferences, maybe also adding some default methods. Eg.:

```java
interface HigherKind1<TC extends HigherKind1<TC, ?>, T> extends __<TC, T> {
  default <R> R transform(Function<__<TC, T>, R> f) {
    return f.apply(this);
  }
}
```
And so you hk encoded classes would look like:
```java
class HkTest<A> implements HigherKind1<HkTest<?>, A> {

}
```

In any case, just try: if you do something wrong the annotation processor shall help you!

## Do not cast! Use the generated safe cast methods
By default the annotation processor will generate a `Hkt` class in each package that contains hk-encoded classes.

The generated class contains casting methods and factories of [TypeEq](blob/master/src/main/java/org/derive4j/hkt/TypeEq.java) that allow you to safely recover the original type from its hk-encoding.

Code generation can be customize by using the [HktConfig](blob/master/src/main/java/org/derive4j/hkt/HktConfig.java) annotation (on package-info or classes).


## Maven
```xml
<dependency>
  <groupId>org.derive4j.hkt</groupId>
  <artifactId>hkt</artifactId>
  <version>0.9.1</version>
</dependency>
```
[search.maven]: http://search.maven.org/#search|ga|1|org.derive4j.hkt

## Gradle
```
compile(group: 'org.derive4j.hkt', name: 'hkt', version: '0.9.1', ext: 'jar')
```