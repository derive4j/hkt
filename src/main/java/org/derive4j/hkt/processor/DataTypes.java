package org.derive4j.hkt.processor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import org.derive4j.Data;
import org.derive4j.Derive;
import org.derive4j.hkt.HktConfig;

import static java.util.stream.Collectors.toList;
import static org.derive4j.hkt.processor.DataTypes.Unit.unit;
import static org.derive4j.hkt.processor._HktConf.Conf;
import static org.derive4j.hkt.processor._HktConf.getClassName;
import static org.derive4j.hkt.processor._HktConf.getCoerceMethodTemplate;
import static org.derive4j.hkt.processor._HktConf.getVisibility;
import static org.derive4j.hkt.processor._HktConf.getWitnessTypeName;

@SuppressWarnings("OptionalGetWithoutIsPresent")
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
                , HktConfig.Visibility visibility
                , String coerceMethodTemplate
                , String witnessTypeName);
        }

        abstract <R> R match(Case<R> Config);

        HktConf mergeWith(HktConf other) {
            return _HktConf.cases()
                .Conf((ocn, ov, omt, own) ->

                    Conf( ocn.equals(getClassName(defaultConfig)) ? getClassName(this) : ocn
                        , ov == getVisibility(defaultConfig) ? getVisibility(this) : ov
                        , omt.equals(getCoerceMethodTemplate(defaultConfig)) ? getCoerceMethodTemplate(this) : omt
                        , own.equals(getWitnessTypeName(defaultConfig)) ? getWitnessTypeName(this) : own))

                .apply(other);
        }

        static HktConf from(HktConfig hktConfig) {
            return Conf(hktConfig.generatedIn()
                , hktConfig.withVisibility()
                , hktConfig.methodNames()
                , hktConfig.witnessTypeName());
        }

        private static final HktConfig defaultHktConfig = defaultConf.class.getAnnotation(HktConfig.class);

        static final HktConf defaultConfig = HktConf.from(defaultHktConfig);

        @HktConfig
        private enum defaultConf {}
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
    static abstract class Valid<E> {
        interface Cases<E, R> {
            R Success(HktDecl hkt);
            R Fail(HktDecl hkt, E error);
        }
        abstract <R> R match(Cases<E, R> cases);

        <EE> Valid<EE> map(Function<E, EE> f) {
            return _Valid.<E>cases()

                .Success(_Valid::<EE>Success)

                .Fail((hkt, error) -> _Valid.Fail(hkt, f.apply(error)))

                .apply(this);
        }

        static Valid<List<HkTypeError>> accumulate(HktDecl hktDecl, Stream<Optional<HkTypeError>> valids) {
            List<HkTypeError> errors = valids.flatMap(o -> o.map(Stream::of).orElseGet(Stream::empty)).collect(toList());
            return errors.isEmpty() ? _Valid.Success(hktDecl) : _Valid.Fail(hktDecl, errors);
        }

        static <E> P2<List<HktDecl>, List<P2<HktDecl, E>>> partition(Stream<Valid<E>> validStream) {
            ArrayList<HktDecl> successes = new ArrayList<>();
            ArrayList<P2<HktDecl, E>> failures = new ArrayList<>();

            IO.sequenceStream_(
                validStream.map(_Valid.<E>cases()
                    .Success(hktDecl -> IO.effect(() -> successes.add(hktDecl)))
                    .Fail((hkt, error) -> IO.effect(() -> failures.add(_P2.of(hkt, error)))))
            ).runUnchecked();

            return _P2.of(successes, failures);
        }
    }

    @FunctionalInterface
    interface IO<A> {

        interface Effect extends IO<Unit> {
            void runEffect() throws IOException;

            @Override
            default Unit run() throws IOException {
                runEffect();
                return unit;
            }
        }

        A run() throws IOException;

        default A runUnchecked() throws UncheckedIOException {
            try {
                return run();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        default <B> IO<B> map(Function<A, B> f) {
            return bind(f.andThen(IO::unit));
        }

        default <B> IO<B> bind(Function<A, IO<B>> f) {
            return () -> f.apply(run()).run();
        }

        static <A> IO<A> unit(A a) { return () -> a; }

        static IO<Unit> effect(Effect eff) {
            return eff;
        }

        static <A> IO<Unit> sequenceStream_(Stream<IO<A>> ios) {
            return () -> {
                try {
                    ios.forEachOrdered(io -> {
                        io.runUnchecked();
                    });
                }
                catch (UncheckedIOException e) {
                    throw e.getCause();
                }
                return unit;
            };
        }
    }

    @Data(@Derive(inClass = "_P2"))
    static abstract class P2<A, B> {
        interface Cases<A, B, R> {
            R of(A _1, B _2);
        }
        abstract <R> R match(Cases<A, B, R> cases);

        A _1() { return _P2.get_1(this); }

        B _2() { return _P2.get_2(this); }

        <C> P2<C, B> map1(Function<A, C> f) {
            return _P2.of(f.apply(_1()), _2());
        }
    }

    enum Unit { unit }
}
