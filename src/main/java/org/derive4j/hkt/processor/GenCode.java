package org.derive4j.hkt.processor;

import java.io.Writer;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.processing.Filer;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.derive4j.hkt.HktConfig;
import org.derive4j.hkt.Leibniz;
import org.derive4j.hkt.processor.DataTypes.HktDecl;
import org.derive4j.hkt.processor.DataTypes.IO;
import org.derive4j.hkt.processor.DataTypes.Opt;
import org.derive4j.hkt.processor.DataTypes.P2;
import org.derive4j.hkt.processor.DataTypes.Unit;

import static java.util.stream.Collectors.joining;
import static org.derive4j.hkt.processor.DataTypes.Unit.unit;

@SuppressWarnings("OptionalGetWithoutIsPresent")
final class GenCode {

    private enum HktEffectiveVisibility {
        Public {
            @Override
            String prefix() {
                return "public ";
            }
        }, Package {
            @Override
            String prefix() {
                return "";
            }
        };

        abstract String prefix();

    }
    private static final String CLASS_TEMPLATE = "package {0};\n" +
        "\n" +
        "import org.derive4j.hkt.*;\n" +
        "\n" +
        "{1}final class {2} '{'\n" +
        "  private {2}() '{'}\n" +
        "\n" +
        "{3}\n" +
        "}";

    private static final String COERCE_METHOD_TEMPLATE = "  {0}static {2} {1} " +
        "{4}({3} hkt) '{'\n" +
        "    return ({1}) hkt;\n" +
        "  }\n" +
        "\n" +
        "  @SuppressWarnings(\"unchecked\")\n" +
        "  {0}static {2} Leibniz<{3}, {1}> {5}()'{'\n" +
        "    return (Leibniz) Leibniz.refl();\n" +
        "  }";

    private final Elements Elts;

    private final Types Types;
    private final Filer Filer;
    private final TypeElement __Elt;
    private final TypeElement TypeEqElt;

    GenCode(Elements elts, Types types, Filer filer, TypeElement elt) {
        Elts = elts;
        Types = types;
        Filer = filer;
        __Elt = elt;
        TypeEqElt = Elts.getTypeElement(Leibniz.class.getName());
    }

    IO<Unit> run(String genClassName, List<HktDecl> hktDecls) {

        PackageElement packageELement = Elts.getPackageOf(_HktDecl.getTypeConstructor(hktDecls.get(0)));

        Set<TypeElement> newElements = hktDecls.stream().map(_HktDecl::getTypeConstructor).collect(Collectors.toSet());

        Stream<P2<HktEffectiveVisibility, String>> existingCoerceMethods = readGenClass(genClassName).map(
            existingGenClasl -> ElementFilter.methodsIn(existingGenClasl.getEnclosedElements())
                .stream()
                .map(method -> parseExistingCoerceMethod(packageELement, method))
                .flatMap(Opt::asStream)
                .filter(p -> !newElements.contains(p._1()))
                .map(P2::_2)).orElseGet(Stream::empty);

        Stream<P2<HktEffectiveVisibility, String>> newCoerceMethods = hktDecls.stream().map(hktDecl -> genCoerceMethod
            (packageELement, hktDecl)).flatMap(Opt::asStream);

        List<P2<HktEffectiveVisibility, String>> allMethods = Stream.concat(existingCoerceMethods, newCoerceMethods)
            .collect(Collectors.toList());

        return allMethods.isEmpty()
            ? IO.unit(unit)
            : generateClass(genClassName, packageELement, allMethods);
    }

    String genClassName(HktDecl hktDecl) {
        return hktDecl.match((typeConstructor, hktInterface, conf) ->
            Elts.getPackageOf(typeConstructor).getQualifiedName() + "." + _HktConf.getClassName(conf));
    }

    private IO<Unit> generateClass(String genClassName, PackageElement packageELement,
        List<P2<HktEffectiveVisibility, String>> allMethods) {

        HktEffectiveVisibility classVisibility = allMethods.stream()
            .map(P2::_1)
            .filter(HktEffectiveVisibility.Public::equals)
            .findAny()
            .orElse(HktEffectiveVisibility.Package);

        String genSimpleClassName = genClassName.substring(packageELement.getQualifiedName().toString().length() + 1,
            genClassName.length());

        String methods = allMethods.stream().map(P2::_2).collect(joining("\n\n"));

        String classContent = MessageFormat.format(CLASS_TEMPLATE, packageELement.getQualifiedName().toString(),
            classVisibility.prefix(), genSimpleClassName, methods);

        return IO.effect(() -> {
            try (Writer classWriter = Filer.createSourceFile(genClassName).openWriter()) {
                classWriter.append(classContent);
                classWriter.flush();
            }
        });
    }

    private Optional<P2<TypeElement, P2<HktEffectiveVisibility, String>>> parseExistingCoerceMethod(PackageElement packageElement,
        ExecutableElement coerceMethod) {
        return coerceMethod.getParameters().size() != 1
            ? Optional.empty()
            : Visitors.asDeclaredType.visit(coerceMethod.getReturnType())
                .filter(dt -> Elts.getPackageOf(dt.asElement()).equals(packageElement))
                .flatMap(declaredType -> allSuperTypes(declaredType).filter(dt -> dt.asElement().equals(__Elt))
                    .findFirst()
                    .flatMap(hktInterface -> Visitors.asTypeElement.visit(declaredType.asElement())
                        .flatMap(typeElement -> ElementFilter.methodsIn(coerceMethod.getEnclosingElement().getEnclosedElements())
                            .stream()
                            .filter(e -> e.getParameters().isEmpty())
                            .filter(e -> e.getReturnType().toString().equals(
                                Types.getDeclaredType(TypeEqElt, hktInterface, typeElement.asType()).toString()))
                            .findAny()
                            .map(typeEqMethod -> _P2.of(typeElement, genCoerceMethod(packageElement, typeElement, hktInterface,
                                coerceMethod.getSimpleName().toString(), typeEqMethod.getSimpleName().toString(),
                                coerceMethod.getModifiers().contains(Modifier.PUBLIC)
                                    ? HktEffectiveVisibility.Public
                                    : HktEffectiveVisibility.Package))))));
    }



    private Optional<P2<HktEffectiveVisibility, String>> genCoerceMethod(PackageElement packageElement, HktDecl hktDecl) {
        return hktDecl.match((typeConstructor, hktInterface, conf) -> {

            HktConfig.Visibility visibility = _HktConf.getVisibility(conf);

            if (visibility == HktConfig.Visibility.Disabled) {
                return Optional.empty();
            }

            final String coerceMethodName =
                _HktConf.getCoerceMethodTemplate(conf)
                    .replace("{ClassName}", typeConstructor.getSimpleName())
                    .replace("{className}", uncapitalize(typeConstructor.getSimpleName()));

            final String typeEqMethodName =
                _HktConf.getTypeEqMethodTemplate(conf)
                    .replace("{ClassName}", typeConstructor.getSimpleName())
                    .replace("{className}", uncapitalize(typeConstructor.getSimpleName()));

            DeclaredType rootHktInterface = allSuperTypes(hktInterface).filter(dt -> dt.asElement().equals(__Elt))
                .findAny()
                .orElse(hktInterface);

            HktEffectiveVisibility effectiveVisibility = visibility == HktConfig.Visibility.Same && typeConstructor.getModifiers()
                .contains(Modifier.PUBLIC) ? HktEffectiveVisibility.Public: HktEffectiveVisibility.Package;

            return Optional.of(genCoerceMethod(packageElement, typeConstructor, rootHktInterface, coerceMethodName,
                typeEqMethodName, effectiveVisibility));
        });
    }

    private P2<HktEffectiveVisibility, String> genCoerceMethod(PackageElement packageElement, TypeElement typeConstructor,
        DeclaredType hktInterface, String coerceMethodName, String typeEqMethodName, HktEffectiveVisibility visibility) {

        String packageNamePrefix = packageElement.getQualifiedName().toString() + ".";

        String typeAsString = typeConstructor.asType()
            .toString()
            .substring(packageNamePrefix.length(), typeConstructor.asType().toString().length());

        TypeElement packageRelativeTypeElement = packageRelativeTypeElement(typeConstructor);

        CharSequence typeParams =  typeConstructor.asType().toString().substring(typeConstructor.getQualifiedName().length());

        String hktInterfaceAsString = hktInterface.toString()
            .replace(Visitors.asTypeElement.visit(hktInterface.asElement()).get().getQualifiedName(), hktInterface
                .asElement().getSimpleName())
            .replace(packageRelativeTypeElement.getQualifiedName(), packageRelativeTypeElement.getSimpleName());

        return _P2.of(visibility,
            MessageFormat.format(COERCE_METHOD_TEMPLATE, visibility.prefix(), typeAsString, typeParams,
                hktInterfaceAsString, coerceMethodName, typeEqMethodName));
    }

    private TypeElement packageRelativeTypeElement(TypeElement typeElement) {
        return typeElement.getEnclosingElement().getKind() == ElementKind.PACKAGE ? typeElement : packageRelativeTypeElement(
            (TypeElement) typeElement.getEnclosingElement());
    }

    private Optional<TypeElement> readGenClass(String genClassName) {
        return Opt.unNull(Elts.getTypeElement(genClassName));
    }

    private Stream<DeclaredType> allSuperTypes(DeclaredType typeMirror) {
        return Types.directSupertypes(typeMirror)
            .stream()
            .map(Visitors.asDeclaredType::visit)
            .flatMap(DataTypes.Opt::asStream)
            .flatMap(s -> Stream.concat(Stream.of(s), allSuperTypes(s)));
    }

    private static String uncapitalize(final CharSequence s) {
        return (s.length() >= 2) && Character.isHighSurrogate(s.charAt(0)) && Character.isLowSurrogate(s.charAt(1))
            ? s.toString().substring(0, 2).toLowerCase(Locale.US) + s.toString().substring(2)
            : s.toString().substring(0, 1).toLowerCase(Locale.US) + s.toString().substring(1);
    }
}
