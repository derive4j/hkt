package org.derive4j.hkt.processor;

import java.io.Writer;
import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.processing.Filer;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.derive4j.hkt.HktConfig;
import org.derive4j.hkt.processor.DataTypes.HktDecl;
import org.derive4j.hkt.processor.DataTypes.IO;
import org.derive4j.hkt.processor.DataTypes.Opt;
import org.derive4j.hkt.processor.DataTypes.P2;
import org.derive4j.hkt.processor.DataTypes.Unit;

import static java.util.stream.Collectors.joining;

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
        "  }";

    private final Elements Elts;
    private final Types Types;
    private final Filer Filer;
    private final TypeElement __Elt;

    GenCode(Elements elts, Types types, Filer filer, TypeElement elt) {
        Elts = elts;
        Types = types;
        Filer = filer;
        __Elt = elt;
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

        Stream<P2<HktEffectiveVisibility, String>> newCoerceMethods = hktDecls.stream().map(hktDecl -> genCoerceMethod(packageELement, hktDecl));

        List<P2<HktEffectiveVisibility, String>> allMethods = Stream.concat(existingCoerceMethods, newCoerceMethods)
            .collect(Collectors.toList());

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


    String genClassName(HktDecl hktDecl) {
        return hktDecl.match((typeConstructor, hktInterface, conf) ->
            Elts.getPackageOf(typeConstructor).getQualifiedName() + "." + _HktConf.getClassName(conf));
    }

    private Optional<P2<TypeElement, P2<HktEffectiveVisibility, String>>> parseExistingCoerceMethod(PackageElement packageElement, ExecutableElement method) {
        return method.getParameters().size() != 1
            ? Optional.empty()
            : Visitors.asDeclaredType.visit(method.getReturnType())
                .filter(dt -> Elts.getPackageOf(dt.asElement()).equals(packageElement))
                .flatMap(declaredType -> allSuperTypes(declaredType).filter(dt -> dt.asElement().equals(__Elt))
                    .findFirst()
                    .flatMap(hktInterface -> Visitors.asTypeElement.visit(declaredType.asElement())
                        .map(typeElement -> _P2.of(typeElement,
                            genCoerceMethod(packageElement, typeElement, hktInterface, method.getSimpleName().toString(),
                                method.getModifiers().contains(Modifier.PUBLIC)
                                    ? HktConfig.Visibility.Same
                                    : HktConfig.Visibility.Package)))));
    }



    private P2<HktEffectiveVisibility, String> genCoerceMethod(PackageElement packageElement, HktDecl hktDecl) {
        return hktDecl.match((typeConstructor, hktInterface, conf) -> {

            final String methodName =
                _HktConf.getCoerceMethodTemplate(conf).replace("{ClassName}", typeConstructor.getSimpleName());

            DeclaredType rootHktInterface = allSuperTypes(hktInterface).filter(dt -> dt.asElement().equals(__Elt))
                .findAny()
                .orElse(hktInterface);

            return genCoerceMethod(packageElement, typeConstructor, rootHktInterface, methodName, _HktConf.getVisibility(conf));
        });
    }

    private P2<HktEffectiveVisibility, String> genCoerceMethod(PackageElement packageElement, TypeElement typeConstructor,
        DeclaredType hktInterface,
        String methodName,
        HktConfig.Visibility visibility) {

        String packageNamePrefix = packageElement.getQualifiedName().toString() + ".";

        String typeAsString = typeConstructor.asType()
            .toString()
            .substring(packageNamePrefix.length(), typeConstructor.asType().toString().length());

        CharSequence typeParams = typeAsString.subSequence(typeConstructor.getSimpleName().length(), typeAsString.length());

        String hktInterfaceAsString = hktInterface.toString()
            .replace(Visitors.asTypeElement.visit(hktInterface.asElement()).get().getQualifiedName(), hktInterface
                .asElement().getSimpleName())
            .replace(typeConstructor.getQualifiedName(), typeConstructor.getSimpleName());

        HktEffectiveVisibility methodVisibility = visibility == HktConfig.Visibility.Same && typeConstructor.getModifiers().contains(Modifier
            .PUBLIC) ? HktEffectiveVisibility.Public: HktEffectiveVisibility.Package;

        return _P2.of(methodVisibility,
            MessageFormat.format(COERCE_METHOD_TEMPLATE, methodVisibility.prefix(), typeAsString, typeParams,
                hktInterfaceAsString, methodName));
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
}
