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

    static <f, A, B, C, D, E> __5<f, A, B, C, D, E> coerce(__4<__<f, A>, B, C, D, E> fabcde) {
        return (__5<f, A, B, C, D, E>) fabcde;
    }

    static <f, A, B, C, D, E> __5<f, A, B, C, D, E> coerce(__<__<__<__<__<f, A>, B>, C>, D>, E> fabcde) {
        return coerce(__4.coerce(__3.coerce(__2.coerce(fabcde))));
    }
}
