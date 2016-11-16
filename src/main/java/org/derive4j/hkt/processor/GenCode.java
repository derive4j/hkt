package org.derive4j.hkt.processor;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.derive4j.hkt.HktConfig;
import org.derive4j.hkt.processor.DataTypes.HktDecl;
import org.derive4j.hkt.processor.DataTypes.IO;
import org.derive4j.hkt.processor.DataTypes.Opt;
import org.derive4j.hkt.processor.DataTypes.P2;
import org.derive4j.hkt.processor.DataTypes.Unit;

@SuppressWarnings("OptionalGetWithoutIsPresent")
final class GenCode {

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
            final HktDecl firstHkt = hktDecls.get(0);
            final PackageElement genClassPkg = Elts.getPackageOf(_HktDecl.getTypeConstructor(firstHkt));
            final TypeSpec genClass = genClass(firstHkt);

            final TypeSpec classToGen = hktDecls
                .subList(1, hktDecls.size())
                .stream()
                .reduce(genClass
                    , this::completeClass
                    , (cl, __) -> cl);

            return createClass(genClassPkg, Opt.cata(readGenClass(genClassName)
                , tel -> enrichClass(tel, classToGen)
                , () -> classToGen));
    }


    String genClassName(HktDecl hktDecl) {
        return hktDecl.match((typeConstructor, hktInterface, conf) ->
            Elts.getPackageOf(typeConstructor).getQualifiedName() + "." + _HktConf.getClassName(conf));
    }

    private IO<Unit> createClass(PackageElement pkg, TypeSpec genClass) {
        return () -> {
            JavaFile
                .builder(pkg.getQualifiedName().toString(), genClass)
                .build()
                .writeTo(Filer);

            return Unit.unit;
        };
    }

    private TypeSpec genClass(HktDecl hktDecl) {
        return hktDecl.match((typeConstructor, hktInterface, conf) -> {
            final boolean isPublic =
                _HktConf.getVisibility(conf) == HktConfig.Visibility.Same
                    && typeConstructor.getModifiers().stream().anyMatch(m -> m == Modifier.PUBLIC);

            final Modifier[] modifiers = isPublic
                ? new Modifier[]{ Modifier.FINAL, Modifier.PUBLIC }
                : new Modifier[]{ Modifier.FINAL };

            return TypeSpec
                .classBuilder(_HktConf.getClassName(conf))
                .addModifiers(modifiers)
                .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build())
                .addMethod(genMethod(hktDecl))
                .build();
        });
    }

    private MethodSpec genMethod(HktDecl hktDecl) {
        return hktDecl.match((typeConstructor, hktInterface, conf) -> {
            final String methodName =
                _HktConf.getCoerceMethodTemplate(conf).replace("{ClassName}", typeConstructor.getSimpleName());

            final List<? extends TypeParameterElement> typeParameters = typeConstructor.getTypeParameters();

            final TypeName argType = TypeName.get(curriedHktInterface(hktInterface));

            final String argName = "hkt";

            final TypeName returnType = TypeName.get
                (Types.getDeclaredType(typeConstructor, typeParameters.stream()
                    .map(TypeParameterElement::asType)
                    .toArray(TypeMirror[]::new)));

            return buildCoerceMethod(methodName, typeParameters, argType, argName, returnType);
        });
    }

    private static MethodSpec buildCoerceMethod(String methodName
        , List<? extends TypeParameterElement> typeParameters
        , TypeName argType
        , String argName
        , TypeName returnType) {
        return MethodSpec
            .methodBuilder(methodName)
            .addModifiers(Modifier.STATIC)
            .addTypeVariables(typeParameters.stream().map(TypeVariableName::get).collect(Collectors.toSet()))
            .returns(returnType)
            .addParameter(argType, argName, Modifier.FINAL)
            .addCode("return ($L) $N;\n", returnType.toString(), argName)
            .build();
    }

    private TypeSpec completeClass(TypeSpec spec, HktDecl hktDecl) {
        return spec.toBuilder().addMethod(genMethod(hktDecl)).build();
    }

    private TypeSpec enrichClass(TypeElement elt, TypeSpec spec) {
        final Stream<MethodSpec> methods = ElementFilter
            .methodsIn(elt.getEnclosedElements())
            .stream()
            .map(method -> {
                final String methodName = method.getSimpleName().toString();
                final List<? extends TypeParameterElement> typeParameters = method.getTypeParameters();
                final P2<String, TypeName> arg = method
                    .getParameters()
                    .stream()
                    .reduce(_P2.of("", TypeName.VOID)
                        , (__, p) -> _P2.of(p.getSimpleName().toString(), TypeName.get(p.asType()))
                        , (pair, __) -> pair);
                final TypeName returnType = TypeName.get(method.getReturnType());

                return buildCoerceMethod(methodName, typeParameters, arg._2(), arg._1(), returnType);
            })
            .filter(ms1 -> spec.methodSpecs.stream().noneMatch(ms2 -> methodEq(ms1, ms2)));

        return spec
            .toBuilder()
            .addMethods(methods.collect(Collectors.toList()))
            .build();
    }

    private static boolean methodEq(MethodSpec m1, MethodSpec m2) {
        return m1.name.equals(m2.name) && m1.returnType.equals(m2.returnType);
    }

    private Optional<TypeElement> readGenClass(String genClassName) {
        return Opt.unNull(Elts.getTypeElement(genClassName));
    }

    private DeclaredType curriedHktInterface(DeclaredType hktInterface) {
        final List<? extends TypeMirror> targs = hktInterface.getTypeArguments();

        final DeclaredType startType = Types.getDeclaredType(__Elt, targs.get(0), targs.get(1));

        return targs.subList(2, targs.size())
            .stream()
            .reduce(startType
                , (declaredType, o) -> Types.getDeclaredType(__Elt, declaredType, o)
                , (declaredType, __) -> declaredType);
    }
}
