package org.derive4j.hkt;

/**
 * Represents a higher order type (or higher kinded type or type constructor) of arity 4
 * @param <f> the 'witness' type (represented by an inner 'µ' class by the implementor) of the type to be lifted as a type constructor
 * @param <A> the 1st type parameter of the type constructor
 * @param <B> the 2nd type parameter of the type constructor
 * @param <C> the 3rd type parameter of the type constructor
 * @param <D> the 4th type parameter of the type constructor
 */
public interface __4<f, A, B, C, D> extends __3<__<f, A>, B, C, D> {

    static <f, A, B, C, D> __4<f, A, B, C, D> coerce(__3<__<f, A>, B, C, D> fabcd) {
        return (__4<f, A, B, C, D>) fabcd;
    }

    static <f, A, B, C, D> __4<f, A, B, C, D> coerce(__<__<__<__<f, A>, B>, C>, D> fabcd) {
        return coerce(__3.coerce(__2.coerce(fabcd)));
    }
}
