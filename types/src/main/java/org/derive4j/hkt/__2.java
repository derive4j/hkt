package org.derive4j.hkt;

public interface __2<f, A, B>  extends __<__<f, A>, B> {

    static <f, A, B> __2<f, A, B> coerce(__<__<f, A>, B> fab) {
        return (__2<f, A, B>) fab;
    }
}
