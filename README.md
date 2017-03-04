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
class HkTest<A> implements __<HkTest<?>, A> {...}
```
and
```java
class HkTest<A> implements __<HkTest.w, A> {
  enum w { // could be any name, also could be a static nested class.
  }
}
```
We say that `__<HkTest.w, A>` is the HK encoding of HkTest<A> and call `w` the *witness type* of `HkTest`.

## What about binary type constructors ? Ternary ? And more ?

@derive4j/hkt supplies interfaces `__<f, A>`, `__2<f, A, B>` up to `__9<f, A, B, C, D, E, F, G, H, I>`.

For example, a disjoint union type commonly called "Either" could be declared this way :
```java
class Either<A, B> implements __2<Either.µ, A, B> {
  enum µ {}
  ...
}
```

## Obligatory Monad example:
The higher kinded polymorphism gained by the encoding allows us to express things that are normally inexpressible in Java. Eg.:
```java
public interface Monad<m> {
  <A> __<m, A> pure(A a);

  <A, B> __<m, B> bind(__<m, A> ma, Function<A, __<m, B>> f);

  default <A, B> __<m, B> map(__<m, A> ma, Function<A, B> f) {
    return bind(ma, f.andThen(this::pure));
  }
}
```

## Aliases interfaces
You may want to create aliases of derive4j hkt `__*` interfaces that better suit your naming preferences, maybe also adding
some default methods. Eg.:

```java
interface HigherKind1<TC extends HigherKind1<TC, ?>, T> extends __<TC, T> {
  default <R> R transform(Function<__<TC, T>, R> f) {
    return f.apply(this);
  }
}
```
And so your hk-encoded classes would look like:
```java
class HkTest<A> implements HigherKind1<HkTest<?>, A> {...}
```
In any case, just try: if you do something wrong the annotation processor shall help you!

## A note on safety : do not cast! Use the generated safe cast methods
By default the annotation processor will generate a `Hkt` class in each package that contains hk-encoded classes.

The generated class contains casting methods and factories of [TypeEq](src/main/java/org/derive4j/hkt/TypeEq.java) that allow you to safely recover the original type from its hk-encoding.

Here is an example :

- given the HKT types
```java
class Maybe<A> implements __<Maybe.µ, A> {...}
```
and
```java
class List<A> implements __<List.µ, A> {...}
```
both in package `myorg.data`

- then the following class will be generated
```java
package myorg.data;

final class Hkt {
  private Hkt() {}
  
  static <A> Maybe<A> asMaybe(final __<Maybe.µ, A> hkt) {
    return (Maybe<A>) hkt;
  }
  
  static <A> List<A> asList(final __<List.µ, A> hkt) {
    return (List<A>) hkt;
  }
}
```

Now you may ask : why is that safe ? I could implement `__<Maybe.µ, A>` in my `Foo<A>` class, pass an instance of it to `Hkt.asMaybe` and then boom !

And to this the answer is no, you can't. That's the whole point of the hkt processor : would you try to implement `__<Maybe.µ, A>` in any other class than `Maybe`, you'd get a **compile time** error.

The processor thus ensures that the only possible implementation of `__<Maybe.µ, A>` is `Maybe<A>` : hence the safety of the cast in the generated methods.

## Configuration of code generation

Code generation can be customized by using the [HktConfig](src/main/java/org/derive4j/hkt/HktConfig.java) annotation (on
package-info or classes).

Consider the example of the previous section : we would like the generated methods to be called `toX` instead of `asX`. Easy ! Just declare, in the `myorg.data` package, a `package-info` file as such :
```java
@HktConfig(coerceMethodName = "to{ClassName}")
package myorg.data;
```

Note that configuration is handled hierarchically through packages, classes and inner classes. That means that would you want to keep your `toX` methods and at the same time have the one for `List` generated in its own class, you could declare a `package-info` as afore mentionned and then annotate the `List` class this way:
```java
@HktConfig(generateIn = "MyHktList")
class List<A> implements __<List.µ, A> {...}
```
As expected, the following two files would then be generated :
```java
package myorg.data;

final class Hkt {
  private Hkt() {}
  
  static <A> Maybe<A> toMaybe(final __<Maybe.µ, A> hkt) {
    return (Maybe<A>) hkt;
  }
}
```
and
```java
package myorg.data;

final class MyHktList {
  private MyHktList() {}
  
  static <A> List<A> toList(final __<List.µ, A> hkt) {
    return (List<A>) hkt;
  }
}
```

## I want it !

### Maven
```xml
<dependency>
  <groupId>org.derive4j.hkt</groupId>
  <artifactId>hkt</artifactId>
  <version>0.9.2</version>
</dependency>
```
[search.maven]: http://search.maven.org/#search|ga|1|org.derive4j.hkt

### Gradle
```
compile(group: 'org.derive4j.hkt', name: 'hkt', version: '0.9.2', ext: 'jar')
```
