package org.derive4j.hkt;

/**
 * Represents a higher order type (or higher kinded type or type constructor) of arity 3
 * @param <f> the 'witness' of the class/interface to be lifted as type constructor: a static nested class or enum, or the class applied with wildcard type arguments.
 * @param <A> the 1st type parameter of the type constructor
 * @param <B> the 2nd type parameter of the type constructor
 * @param <C> the 3rd type parameter of the type constructor
 */
public interface __3<f, A, B, C> extends __2<__<f, A>, B, C> {}
