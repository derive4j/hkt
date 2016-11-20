package org.derive4j.hkt;

/**
 * Represents a higher order type (or higher kinded type or type constructor) of arity 8.
 * @param <f> the 'witness' of the class/interface to be lifted as type constructor: a static nested class or enum, or the class applied with wildcard type arguments.
 * @param <A> the 1st type parameter of the type constructor
 * @param <B> the 2nd type parameter of the type constructor
 * @param <C> the 3rd type parameter of the type constructor
 * @param <D> the 4th type parameter of the type constructor
 * @param <E> the 5th type parameter of the type constructor
 * @param <F> the 6th type parameter of the type constructor
 * @param <G> the 7th type parameter of the type constructor
 * @param <H> the 8th type parameter of the type constructor
 */
public interface __8<f, A, B, C, D, E, F, G, H> extends __7<__<f, A>, B, C, D, E, F, G, H> {}
