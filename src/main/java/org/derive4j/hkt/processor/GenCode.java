package org.derive4j.hkt.processor;

import org.derive4j.hkt.HktConfig;
import org.derive4j.hkt.TypeEq;
import org.derive4j.hkt.processor.DataTypes.*;

import javax.annotation.processing.Filer;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
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
        "import org.derive4j.hkt.__;\n" +
        "import org.derive4j.hkt.TypeEq;\n" +
        "import io.kindedj.Hk;\n" +
        "{3}\n"+
        "\n" +
        "{1}final class {2} '{'\n" +
        "  private {2}() '{'}\n" +
        "\n" +
        "{4}\n" +
        "}";

    private static final String METHODS_TEMPLATE = "  @SuppressWarnings(\"unchecked\")\n" +
        "  {0}static {2} {1} {5}({3} hkt) '{'\n" +
        "    return ({1}) hkt;\n" +
        "  }\n" +
        "\n" +
        "  @SuppressWarnings(\"unchecked\")\n" +
        "  {0}static {2} TypeEq<{4}, {1}> {6}()'{'\n" +
        "    return (TypeEq) TypeEq.refl();\n" +
        "  }";

    private static final String TYPE_PARAMS_TEMPLATE = "<{0}>";

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
        TypeEqElt = Elts.getTypeElement(TypeEq.class.getName());
    }

    IO<Unit> run(String genClassName, List<HktDecl> hktDecls) {

        Set<TypeElement> newTypeElements = hktDecls.stream().map(_HktDecl::getTypeConstructor).collect(Collectors.toSet());

        List<P2<TypeElement, P2<HktEffectiveVisibility, String>>> existingCoerceMethods = readGenClass(genClassName).map(
            existingGenClasl -> ElementFilter.methodsIn(existingGenClasl.getEnclosedElements())
                .stream()
                .map(this::parseExistingCoerceMethod)
                .flatMap(Opt::asStream)
                .filter(p -> !newTypeElements.contains(p._1())))
            .orElseGet(Stream::empty)
            .collect(toList());

        List<TypeElement> allTypeElements = Stream.concat(existingCoerceMethods.stream().map(P2::_1), hktDecls.stream().map
            (_HktDecl::getTypeConstructor)).collect(toList());

        Stream<P2<HktEffectiveVisibility, String>> newCoerceMethods = hktDecls.stream().map(this::genCoerceMethod).flatMap(Opt::asStream);

        List<P2<HktEffectiveVisibility, String>> allMethods = Stream.concat(existingCoerceMethods.stream().map(P2::_2), newCoerceMethods)
            .collect(Collectors.toList());

        return allMethods.isEmpty()
            ? IO.unit(unit)
            : generateClass(genClassName, allTypeElements, allMethods);
    }

    String genClassName(HktDecl hktDecl) {
        return hktDecl.match((typeConstructor, hktInterface, conf) ->
            _HktConf.getClassName(conf).contains(".")
                ? _HktConf.getClassName(conf)
                : Elts.getPackageOf(typeConstructor).getQualifiedName() + "." + _HktConf.getClassName(conf));
    }

    private IO<Unit> generateClass(String genClassName, List<TypeElement> allTypeElements, List<P2<HktEffectiveVisibility, String>> allMethods) {

        PackageElement packageELement = Elts.getPackageElement(genClassName.substring(0, genClassName.lastIndexOf(".")));

        HktEffectiveVisibility classVisibility = allMethods.stream()
            .map(P2::_1)
            .filter(HktEffectiveVisibility.Public::equals)
            .findAny()
            .orElse(HktEffectiveVisibility.Package);

        String genSimpleClassName = genClassName.substring(packageELement.getQualifiedName().toString().length() + 1,
            genClassName.length());

        String explicitImports = allTypeElements.stream()
            .map(this::packageRelativeTypeElement)
            .filter(te -> !Elts.getPackageOf(te).equals(packageELement))
            .map(te -> "import " + te.toString() + ";")
            .collect(joining("\n"));

        String methods = allMethods.stream().map(P2::_2).collect(joining("\n\n"));

        String classContent = MessageFormat.format(CLASS_TEMPLATE, packageELement.getQualifiedName().toString(),
            classVisibility.prefix(), genSimpleClassName, explicitImports, methods);

        return IO.effect(() -> {
            try (Writer classWriter = new OutputStreamWriter(
                    Filer.createSourceFile(genClassName).openOutputStream(), UTF_8)) {
                classWriter.append(classContent);
                classWriter.flush();
            }
        });
    }

    private Optional<P2<TypeElement, P2<HktEffectiveVisibility, String>>> parseExistingCoerceMethod(ExecutableElement coerceMethod) {
        return coerceMethod.getParameters().size() != 1
            ? Optional.empty()
            : Visitors.asDeclaredType.visit(coerceMethod.getReturnType())
                .flatMap(declaredType -> allSuperTypes(declaredType).filter(dt -> dt.asElement().equals(__Elt))
                    .findFirst()
                    .flatMap(hktInterface -> Visitors.asTypeElement.visit(declaredType.asElement())
                        .flatMap(typeElement -> ElementFilter.methodsIn(coerceMethod.getEnclosingElement().getEnclosedElements())
                            .stream()
                            .filter(e -> e.getParameters().isEmpty())
                            .filter(e -> e.getReturnType().toString().equals(
                                Types.getDeclaredType(TypeEqElt, hktInterface, typeElement.asType()).toString()))
                            .findAny()
                            .map(typeEqMethod -> _P2.of(typeElement, genCoerceMethod(typeElement, hktInterface,
                                coerceMethod.getSimpleName().toString(), typeEqMethod.getSimpleName().toString(),
                                coerceMethod.getModifiers().contains(Modifier.PUBLIC)
                                    ? HktEffectiveVisibility.Public
                                    : HktEffectiveVisibility.Package))))));
    }



    private Optional<P2<HktEffectiveVisibility, String>> genCoerceMethod(HktDecl hktDecl) {
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

            return Optional.of(genCoerceMethod(typeConstructor, rootHktInterface, coerceMethodName,
                typeEqMethodName, effectiveVisibility));
        });
    }

    private P2<HktEffectiveVisibility, String> genCoerceMethod(TypeElement typeConstructor,
        DeclaredType hktInterface, String coerceMethodName, String typeEqMethodName, HktEffectiveVisibility visibility) {

        String packageNamePrefix = Elts.getPackageOf(typeConstructor).getQualifiedName().toString() + ".";

        String typeAsString = typeConstructor.asType()
            .toString()
            .substring(packageNamePrefix.length(), typeConstructor.asType().toString().length());

        TypeElement packageRelativeTypeElement = packageRelativeTypeElement(typeConstructor);

        CharSequence typeParams = MessageFormat.format(TYPE_PARAMS_TEMPLATE, showTypeParams(typeConstructor));

        String hkInterfaceFQN = Visitors.asTypeElement.visit(hktInterface.asElement()).get().getQualifiedName().toString();

        String kindedJInterfaceAsString = hktInterface.toString()
            .replace("<" + hkInterfaceFQN, "<? extends Hk")
            .replace(hkInterfaceFQN, "Hk")
            .replace(packageRelativeTypeElement.getQualifiedName(), packageRelativeTypeElement.getSimpleName());

        String hktInterfaceAsString = hktInterface.toString()
            .replace(hkInterfaceFQN, hktInterface.asElement().getSimpleName())
            .replace("io.kindedj.Hk", "Hk")
            .replace(packageRelativeTypeElement.getQualifiedName(), packageRelativeTypeElement.getSimpleName());

        return _P2.of(visibility,
            MessageFormat.format(METHODS_TEMPLATE, visibility.prefix(), typeAsString, typeParams,
                kindedJInterfaceAsString, hktInterfaceAsString, coerceMethodName, typeEqMethodName));
    }

    private TypeElement packageRelativeTypeElement(TypeElement typeElement) {
        return typeElement.getEnclosingElement().getKind() == ElementKind.PACKAGE ? typeElement : packageRelativeTypeElement(
            (TypeElement) typeElement.getEnclosingElement());
    }

    private Optional<TypeElement> readGenClass(String genClassName) {
        return Opt.unNull(Elts.getTypeElement(genClassName));
    }

    private Stream<DeclaredType> allSuperTypes(DeclaredType typeMirror) {
        return Visitors.allSuperTypes(Types, typeMirror);
    }

    private static String showTypeParams(TypeElement te) {
        return te
            .getTypeParameters()
            .stream()
            .map(GenCode::showTypeParam)
            .collect(joining(", "));
    }

    private static String showTypeParam(TypeParameterElement tpe) {
        final String bounds = tpe.getBounds()
            .stream()
            .map(TypeMirror::toString)
            .filter(s -> !s.contentEquals("java.lang.Object"))
            .collect(joining(", "));

        return tpe.asType().toString() + (bounds.isEmpty() ? "" : " extends " + bounds);
    }

    private static String uncapitalize(final CharSequence s) {
        return (s.length() >= 2) && Character.isHighSurrogate(s.charAt(0)) && Character.isLowSurrogate(s.charAt(1))
            ? s.toString().substring(0, 2).toLowerCase(Locale.US) + s.toString().substring(2)
            : s.toString().substring(0, 1).toLowerCase(Locale.US) + s.toString().substring(1);
    }
}
