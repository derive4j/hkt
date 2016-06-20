package org.derive4j.hkt;

/**
 * Represents a higher order type (or higher kinded type or type constructor) of arity 3
 * @param <f> the 'witness' type (represented by an inner 'µ' class by the implementor) of the type to be lifted as a type constructor
 * @param <A> the 1st type parameter of the type constructor
 * @param <B> the 2nd type parameter of the type constructor
 * @param <C> the 3rd type parameter of the type constructor
 */
public interface __3<f, A, B, C> extends __2<__<f, A>, B, C> {

  static <f, A, B, C> __3<f, A, B, C> coerce(__<__<__<f, A>, B>, C> fabc) {
    return (__3<f, A, B, C>) fabc;
  }

  /**
   * Reify the higher kinded type equality:<pre>
   * {@code __<__<__<f, A>, B>, C>}  =:= {@code __3<f, A, B, C>}</pre>
   * that is guaranteed by the hkt type checker.
   *
   * @param <A> the antepenultimate type variable
   * @param <B> before last type variable.
   * @param <C> last type variable.
   * @return a leibniz instance witness of the type equality.
   */
  @SuppressWarnings("unchecked")
  static <f, A, B, C> Leibniz<__<__<__<f, A>, B>, C>, __3<f, A, B, C>> eq__() {
    return (Leibniz) Leibniz.refl();
  }

  /**
   * Reify the higher kinded type equality:<pre>
   * {@code __2<__<f, A>, B, C>}  =:= {@code __3<f, A, B, C>}</pre>
   * that is guaranteed by the hkt type checker.
   *
   * @param <A> the antepenultimate type variable
   * @param <B> before last type variable.
   * @param <C> last type variable.
   * @return a leibniz instance witness of the type equality.
   */
  @SuppressWarnings("unchecked")
  static <f, A, B, C> Leibniz<__2<__<f, A>, B, C>, __3<f, A, B, C>> eq__2() {
    return (Leibniz) Leibniz.refl();
  }
}
