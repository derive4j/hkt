package org.derive4j.hkt;

import io.kindedj.Hk;

/**
 * Represents a higher order type (or higher kinded type or type constructor) of arity 1
 * @param <f> the 'witness' of the class/interface to be lifted as type constructor: a static nested class or enum, or the class applied with wildcard type arguments.
 * @param <T> the type parameter of the type constructor
 */
public interface __<f, T> extends Hk<f, T> {}
