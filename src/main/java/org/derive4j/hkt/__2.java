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

  /**
   * Reify the higher kinded type equality:<pre>
   * {@code __2<f, A, B>  =:= __<__<f, A>, B>}</pre>
   * that is guaranteed by the hkt type checker.
   *
   * @param <A> before last type variable.
   * @param <B> last type variable.
   * @return a leibniz instance witness of the type equality.
   */
  @SuppressWarnings("unchecked")
  static <f, A, B> Leibniz<__<__<f, A>, B>, __2<f, A, B>> eq__() {
    return (Leibniz) Leibniz.refl();
  }
}
