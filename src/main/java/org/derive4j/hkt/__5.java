package org.derive4j.hkt;

/**
 * Represents a higher order type (or higher kinded type or type constructor) of arity 5
 * @param <f> the 'witness' type (represented by an inner 'µ' class by the implementor) of the type to be lifted as a type constructor
 * @param <A> the 1st type parameter of the type constructor
 * @param <B> the 2nd type parameter of the type constructor
 * @param <C> the 3rd type parameter of the type constructor
 * @param <D> the 4th type parameter of the type constructor
 * @param <E> the 5th type parameter of the type constructor
 */
public interface __5<f, A, B, C, D, E> extends __4<__<f, A>, B, C, D, E> {

  static <f, A, B, C, D, E> __5<f, A, B, C, D, E> coerce(
      __<__<__<__<__<f, A>, B>, C>, D>, E> fabcde) {
    return (__5<f, A, B, C, D, E>) fabcde;
  }

  /**
   * Reify the higher kinded type equality:<pre>
   * {@code __<__<__<__<__<f, A>, B>, C>, D>, E>}  =:= {@code __4<f, A, B, C, D>}</pre>
   * that is guaranteed by the hkt type checker.
   *
   * @return a TypeEq instance witness of the type equality.
   */
  @SuppressWarnings("unchecked")
  static <f, A, B, C, D, E> TypeEq<__<__<__<__<__<f, A>, B>, C>, D>, E>, __5<f, A, B, C, D, E>> eq__() {
    return (TypeEq) TypeEq.refl();
  }

  /**
   * Reify the higher kinded type equality:<pre>
   * {@code __4<__<f, A>, B, C, D, E>}  =:= {@code __5<f, A, B, C, D, E>}</pre>
   * that is guaranteed by the hkt type checker.
   *
   * @return a TypeEq instance witness of the type equality.
   */
  @SuppressWarnings("unchecked")
  static <f, A, B, C, D, E> TypeEq<__4<__<f, A>, B, C, D, E>, __5<f, A, B, C, D, E>> eq__4() {
    return (TypeEq) TypeEq.refl();
  }
}
