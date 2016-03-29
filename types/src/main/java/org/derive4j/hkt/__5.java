package org.derive4j.hkt;

public interface __5<f, A, B, C, D, E> extends __4<__<f, A>, B, C, D, E> {

    static <f, A, B, C, D, E> __5<f, A, B, C, D, E> coerce(__4<__<f, A>, B, C, D, E> fabcde) {
        return (__5<f, A, B, C, D, E>) fabcde;
    }

    static <f, A, B, C, D, E> __5<f, A, B, C, D, E> coerce(__<__<__<__<__<f, A>, B>, C>, D>, E> fabcde) {
        return coerce(__4.coerce(__3.coerce(__2.coerce(fabcde))));
    }
}
