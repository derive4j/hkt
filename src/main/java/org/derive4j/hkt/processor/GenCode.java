package org.derive4j.hkt.processor;

import com.squareup.javapoet.*;
import org.derive4j.hkt.Hkt;
import org.derive4j.hkt.__;
import org.derive4j.hkt.processor.DataTypes.HktDecl;
import org.derive4j.hkt.processor.DataTypes.IO;
import org.derive4j.hkt.processor.DataTypes.Unit;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

final class GenCode {
    private final Elements Elts;
    private final Types Types;
    private final Filer Filer;

    GenCode(Elements elts, Types types, Filer filer) {
        Elts = elts;
        Types = types;
        Filer = filer;
    }

    IO<Unit> run(HktDecl hktDecl) {
        final String genClassName = genClassName(hktDecl);

        return genClassFile(genClassName)

            .bind(ofo -> IO.sequenceOpt(ofo.map(this::delete))

                .bind(ou -> createClass(hktDecl, genClass(hktDecl))));
    }

    private IO<Unit> createClass(HktDecl hktDecl, TypeSpec genClass) {
        return hktDecl.match((typeConstructor, hktInterface, conf) -> () -> {

            System.out.println("Creating class");

            final String pkgName =
                Elts.getPackageOf(typeConstructor).getQualifiedName().toString();

            JavaFile.builder(pkgName, genClass).build().writeTo(Filer);

            return Unit.unit;
        });
    }

    private TypeSpec genClass(HktDecl hktDecl) {
        return hktDecl.match((typeConstructor, hktInterface, conf) -> {
            final boolean isPublic =
                _HktConf.getVisibility(conf) == Hkt.Visibility.Same
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

            final DeclaredType returnType =
                Types.getDeclaredType(typeConstructor, typeParameters.stream()
                    .map(TypeParameterElement::asType)
                    .toArray(TypeMirror[]::new));

            return MethodSpec
                .methodBuilder(methodName)
                .addModifiers(Modifier.STATIC)
                .addTypeVariables(typeParameters.stream().map(TypeVariableName::get).collect(Collectors.toSet()))
                .returns(TypeName.get(returnType))
                .addParameter(argType, argName, Modifier.FINAL)
                .addCode("return ($L) $N;\n", returnType.toString(), argName)
                .build();
        });
    }

    private String genClassName(HktDecl hktDecl) {
        return hktDecl.match((typeConstructor, hktInterface, conf) ->
            Elts.getPackageOf(typeConstructor).getQualifiedName() + "." + _HktConf.getClassName(conf));
    }

//    private IO<Optional<?>> genClassFile2(String genClassName) {
//        return () -> {
//          try {
//              return Optional.of(Elts.getTypeElement(genClassName));
//          }
//        };
//    }

    private IO<Optional<FileObject>> genClassFile(String genClassName) {
        return () -> {

            System.out.println("Reading File : " + genClassName);

            final FileObject fileObject =
                Filer.getResource(StandardLocation.SOURCE_OUTPUT, "", genClassName);
            try {
                fileObject.getCharContent(true); // force loading (detection) of the file
                return Optional.of(fileObject);
            } catch (FileNotFoundException e) {
                return Optional.empty();
            }
        };
    }

    private IO<Unit> delete(FileObject genClass) {
        return () -> {

            System.out.println("Deleting file");

            if (genClass.delete()) return Unit.unit;
            else throw new IOException("Could not delete a generated class");
        };
    }

    private DeclaredType curriedHktInterface(DeclaredType hktInterface) {
        final TypeElement __Elt = Elts.getTypeElement(__.class.getCanonicalName());

        final List<? extends TypeMirror> targs = hktInterface.getTypeArguments();

        final DeclaredType startType = Types.getDeclaredType(__Elt, targs.get(0), targs.get(1));

        return targs.subList(2, targs.size())
            .stream()
            .reduce(startType
                , (declaredType, o) -> Types.getDeclaredType(__Elt, declaredType, o)
                , (declaredType, __) -> declaredType);
    }
}
