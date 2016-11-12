package org.derive4j.hkt.processor;

import org.derive4j.Data;
import org.derive4j.Derive;
import org.derive4j.hkt.Hkt;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.unmodifiableSet;
import static org.derive4j.hkt.processor._HktConf.*;

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

        static Function<HkTypeError, Valid<HkTypeError>> Fail(HktDecl hktDecl) {
            return error -> _Valid.Fail(hktDecl, error);
        }

        private static Valid<Stream<HkTypeError>> accum(Valid<Stream<HkTypeError>> acc, Valid<HkTypeError> v) {
            return _Valid.<Stream<HkTypeError>>cases()

                .Success(__ -> v.map(Stream::of))

                .Fail((hkt, errors) -> _Valid.<HkTypeError>cases()
                    .Success(__ -> acc)
                    .Fail((__, error) -> _Valid.Fail(hkt, Stream.concat(errors, Stream.of(error))))
                    .apply(v))

                .apply(acc);
        }

        static Valid<Stream<HkTypeError>> accumulate(HktDecl hktDecl, Stream<Valid<HkTypeError>> valids) {
            return _Valid.lazy(() -> valids.reduce(_Valid.Success(hktDecl), Valid::accum, (v, __) -> v));
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

    @Data(@Derive(inClass = "_Action"))
    static abstract class Action<T> {
        interface Cases<R, T> {
            R GenCode(String className, HktDecl hkt, Function<GenCode, T> id);
            R ReportError(HktDecl hkt, Stream<HkTypeError> errors, Function<HkTypeError, T> id);
        }
        abstract <R> R match(Cases<R, T> cases);

        static boolean classNameEq(Action<GenCode> a1, Action<GenCode> a2) {
            return _Action.getClassName(a1).get().equals(_Action.getClassName(a2).get());
        }

        static final Comparator<Action<GenCode>> byClassName =
            Comparator.comparing(ac -> _Action.getClassName(ac).get());

        static <T> P2<Stream<Action<GenCode>>, Stream<Action<HkTypeError>>> partition(Stream<Action<T>> as) {
            return as.reduce(_P2.of(Stream.empty(), Stream.empty())

                , (pair, a) -> _Action.<T>cases()
                    .GenCode((className, hkt, __) ->
                        _P2.of(Stream.concat(pair._1(), Stream.of(_Action.GenCode(className, hkt))), pair._2()))

                    .ReportError((hkt, errors, __) ->
                        _P2.of(pair._1(), Stream.concat(pair._2(), Stream.of(_Action.ReportError(hkt, errors)))))

                    .apply(a)

                , (pair, __) -> pair);
        }
    }

    static class StreamOps {

        static <A> Optional<A> head(Stream<A> as) {
            return as.findFirst();
        }

        static <A> P2<Stream<A>, Stream<A>> span(Stream<A> as, Function<A, Boolean> p) {
            final List<A> buf = new ArrayList<>();

            for (List<A> xs = as.collect(Collectors.toList()); !xs.isEmpty(); xs = xs.subList(1, xs.size()))
                if (p.apply(xs.get(0)))
                    buf.add(xs.get(0));
                else
                    return _P2.of(buf.stream(), xs.stream());

            return _P2.of(buf.stream(), Stream.empty());
        }

        static <A> Stream<Stream<A>> group(Stream<A> as, BiFunction<A, A, Boolean> p) {
            final List<A> las = as.collect(Collectors.toList());

            if (las.isEmpty())
                return Stream.empty();
            else {
                final P2<Stream<A>, Stream<A>> z =
                    span(las.subList(1, las.size()).stream(), a -> p.apply(a, las.get(0)));
                return Stream.concat(Stream.of(Stream.concat(Stream.of(las.get(0)), z._1())), group(z._2(), p));
            }
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
