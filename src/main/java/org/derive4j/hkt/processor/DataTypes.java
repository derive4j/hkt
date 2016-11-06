package org.derive4j.hkt.processor;

import org.derive4j.Data;
import org.derive4j.Derive;
import org.derive4j.hkt.Hkt;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.unmodifiableSet;
import static org.derive4j.hkt.processor._HktConf.*;

final class DataTypes {
    private DataTypes() {}

    @Data(@Derive(inClass = "_HktDecl"))
    static abstract class HktDecl {
        interface Cases<R> {
            R of(TypeElement typeConstructor, DeclaredType hktInterface, HktConf conf);
        }
        abstract <R> R match(Cases<R> cases);
    }

    @Data(@Derive(inClass = "_HkTypeError"))
    abstract static class HkTypeError {
        interface Cases<R> {
            R HKTInterfaceDeclIsRawType();
            R HKTypesNeedAtLeastOneTypeParameter();
            R WrongHKTInterface();
            R NotMatchingTypeParams(List<TypeParameterElement> missingOrOutOfOrderTypeArguments);
            R TCWitnessMustBeNestedClassOrClass();
            R NestedTCWitnessMustBeSimpleType(TypeElement tcWitnessElement);
            R NestedTCWitnessMustBeStaticFinal(TypeElement tcWitnessElement);
        }

        abstract <R> R match(Cases<R> cases);
    }

    @Data(@Derive(inClass = "_HktConf"))
    abstract static class HktConf {
        interface Case<R> {
            R Conf(String className
                , Hkt.Visibility visibility
                , String coerceMethodTemplate
                , String witnessTypeName
                , Set<Hkt.Generator> codeGenerator);
        }

        abstract <R> R match(Case<R> Config);

        HktConf mergeWith(HktConf other) {
            return _HktConf.cases()
                .Conf((ocn, ov, omt, own, ocg) ->

                    Conf( ocn.equals(getClassName(defaultConfig)) ? getClassName(this) : ocn
                        , ov == getVisibility(defaultConfig) ? getVisibility(this) : ov
                        , omt.equals(getCoerceMethodTemplate(defaultConfig)) ? getCoerceMethodTemplate(this) : omt
                        , own.equals(getWitnessTypeName(defaultConfig)) ? getWitnessTypeName(this) : own
                        , ocg.equals(getCodeGenerator(defaultConfig)) ? getCodeGenerator(this) : ocg))

                .apply(other);
        }

        static HktConf from(Hkt hkt) {
            return Conf(hkt.generatedIn()
                , hkt.withVisibility()
                , hkt.methodNames()
                , hkt.witnessTypeName()
                , toSet(hkt.delegateTo()));
        }

        private static final Hkt defaultHkt = defaultConf.class.getAnnotation(Hkt.class);

        static final HktConf defaultConfig = HktConf.from(defaultHkt);

        @Hkt
        private enum defaultConf {}

        private static Set<Hkt.Generator> toSet(Hkt.Generator[] gens) {
            return unmodifiableSet(Arrays.stream(gens).collect(Collectors.toSet()));
        }
    }

    static final class Opt {
        private Opt() {}

        static <T> Optional<T> unNull(T t) { return Optional.ofNullable(t); }

        static Optional<String> fromStr(String str) {
            return unNull(str).flatMap(s -> s.length() == 0 ? Optional.empty() : Optional.of(s));
        }

        static <T, R> R cata(Optional<T> opt, Function<T, R> f, Supplier<R> r) {
            return opt.map(f).orElseGet(r);
        }

        static <T> Optional<T> or(Optional<T> ot, Supplier<Optional<T>> or) {
            return cata(ot, Optional::of, or);
        }

        static <A> Stream<A> asStream(Optional<A> oa) { return cata(oa, Stream::of, Stream::empty); }
    }

    @Data(@Derive(inClass = "_Valid"))
    static abstract class Valid {
        interface Cases<R> {
            R Success(HktDecl hkt);
            R Fail(HktDecl hkt, HkTypeError error);
        }
        abstract <R> R match(Cases<R> cases);

        static Function<HkTypeError, Valid> Fail(HktDecl hktDecl) {
            return error -> _Valid.Fail(hktDecl, error);
        }
    }

    @FunctionalInterface
    interface IO<A> {
        A run() throws Throwable;

        default <B> IO<B> map(Function<A, B> f) {
            return bind(f.andThen(IO::unit));
        }

        default <B> IO<B> bind(Function<A, IO<B>> f) {
            return () -> f.apply(run()).run();
        }

        static <A> IO<A> unit(A a) { return () -> a; }

        static IO<Unit> effect(Runnable eff) {
            return () -> { eff.run(); return Unit.unit; };
        }

        static <A> Unit unsafeRun(Stream<IO<A>> ios) {
            ios.forEach(io -> {
                try {
                    io.run();
                } catch (Throwable throwable) {
                    throw new RuntimeException(throwable);
                }
            });
            return Unit.unit;
        }

        static <A> IO<Optional<A>> sequenceOpt(Optional<IO<A>> oio) {
            return Opt.cata(oio, io -> io.map(Optional::of), () -> IO.unit(Optional.empty()));
        }
    }

    enum Unit { unit }
}
