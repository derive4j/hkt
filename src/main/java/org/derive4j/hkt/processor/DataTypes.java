package org.derive4j.hkt.processor;

import org.derive4j.Data;
import org.derive4j.hkt.Hkt;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Collections.unmodifiableSet;
import static org.derive4j.hkt.processor.CodeGenConfigs.Config;

final class DataTypes {
    private DataTypes() {}

    @Data
    static abstract class HktToValidate {
        interface Cases<R> {
            R of(TypeElement typeConstructor, DeclaredType hktInterface);
        }
        abstract <R> R match(Cases<R> cases);
    }

    @Data
    abstract static class HkTypeError {
        interface Cases<R> {
            R HKTInterfaceDeclrationIsRawType();
            R HKTypesNeedAtLeastOneTypeParameter();
            R WrongHKTInterface();
            R NotMatchingTypeParams(List<TypeParameterElement> missingOrOutOfOrderTypeArguments);
            R TCWitnessMustBeNestedClassOrClass();
            R NestedTCWitnessMustBeSimpleType(TypeElement tcWitnessElement);
            R NestedTCWitnessMustBeStaticFinal(TypeElement tcWitnessElement);
        }

        abstract <R> R match(Cases<R> cases);
    }

    @Data
    abstract static class CodeGenConfig {
        interface Case<R> {
            R Config(String cassName, Hkt.Visibility visibility, String coerceMethodTemplate, Set<Hkt.Generator> codeGenerator);
        }

        abstract <R> R match(Case<R> Config);

        static final CodeGenConfig defaultConfig = Config("Hkt", Hkt.Visibility.Same, "as{ClassName}", unmodifiableSet(EnumSet.of(Hkt.Generator.derive4j)));
    }

    public static final class Opt {
        private Opt() {}

        static <T> Optional<T> unNull(T t) { return Optional.ofNullable(t); }

        static <T, R> R cata(Optional<T> opt, Function<T, R> f, Supplier<R> r) {
            return opt.map(f).orElseGet(r);
        }

        static <A> Stream<A> asStream(Optional<A> oa) { return cata(oa, Stream::of, Stream::empty); }
    }

    enum Unit { unit }
}
