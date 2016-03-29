package org.derive4j.hkt;

public interface __3<f, A, B, C> extends __2<__<f, A>, B, C> {

    static <f, A, B, C> __3<f, A, B, C> coerce(__2<__<f, A>, B, C> fabc) {
        return (__3<f, A, B, C>) fabc;
    }

    static <f, A, B, C> __3<f, A, B, C> coerce(__<__<__<f, A>, B>, C> fabc) {
        return coerce(__2.coerce(fabc));
    }
}
