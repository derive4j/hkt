package org.derive4j.hkt;

public interface __4<f, A, B, C, D> extends __3<__<f, A>, B, C, D> {

    static <f, A, B, C, D> __4<f, A, B, C, D> coerce(__3<__<f, A>, B, C, D> fabcd) {
        return (__4<f, A, B, C, D>) fabcd;
    }

    static <f, A, B, C, D> __4<f, A, B, C, D> coerce(__<__<__<__<f, A>, B>, C>, D> fabcd) {
        return coerce(__3.coerce(__2.coerce(fabcd)));
    }
}
