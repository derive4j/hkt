package org.derive4j.hkt;

/**
 * Represents a higher order type (or higher kinded type or type constructor) of arity 2
 * @param <f> the 'witness' type (represented by an inner 'µ' class by the implementor) of the type to be lifted as a type constructor
 * @param <A> the 1st type parameter of the type constructor
 * @param <B> the 2nd type parameter of the type constructor
 */
public interface __2<f, A, B> extends __<__<f, A>, B> {

  static <f, A, B> __2<f, A, B> coerce(__<__<f, A>, B> fab) {
    return (__2<f, A, B>) fab;
  }
}
