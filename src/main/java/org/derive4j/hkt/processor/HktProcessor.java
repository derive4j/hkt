/*
    Copyright (c) 2016, Grégoire Neuville and Derive4J HKT contributors.
    All rights reserved.

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this
      list of conditions and the following disclaimer.

    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.

    * Neither the name of hkt nor the names of its
      contributors may be used to endorse or promote products derived from
      this software without specific prior written permission.

    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
    AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
    IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
    DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
    FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
    DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
    SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
    CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
    OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
    OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.derive4j.hkt.processor;

import com.google.auto.service.AutoService;
import org.derive4j.hkt.Hkt;
import org.derive4j.hkt.__;
import org.derive4j.hkt.processor.DataTypes.*;
import org.derive4j.hkt.processor.JavaCompiler.OpenJdkSpecificApi;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.util.*;
import javax.tools.Diagnostic;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.Math.min;
import static java.lang.String.format;
import static java.util.Collections.unmodifiableSet;
import static org.derive4j.hkt.processor.GenCodeConfs.*;
import static org.derive4j.hkt.processor.HkTypeErrors.*;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("*")
public final class HktProcessor extends AbstractProcessor {

    private static final String  µTypeName = "µ";

    private Filer Filer;
    private Types Types;
    private Elements Elts;
    private Messager Messager;
    private Optional<JavaCompiler.JdkSpecificApi> JdkSpecificApi;

    private TypeElement __Elt;

    private TypeElement HktAnnot;

    private ExecutableElement generatedClassNameConfigMethod;
    private ExecutableElement generatedClassVisibilityConfigMethod;
    private ExecutableElement generatedMethodTemplateConfigMethod;
    private ExecutableElement generatedMethodDelegationConfigMethod;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        Filer = processingEnv.getFiler();
        Types = processingEnv.getTypeUtils();
        Elts = processingEnv.getElementUtils();
        Messager = processingEnv.getMessager();
        JdkSpecificApi = jdkSpecificApi(processingEnv);

        __Elt = Elts.getTypeElement(__.class.getCanonicalName());

        HktAnnot = Elts.getTypeElement(Hkt.class.getCanonicalName());
        final List<ExecutableElement> hktMethods = ElementFilter.methodsIn(HktAnnot.getEnclosedElements());
        generatedClassNameConfigMethod = hktMethods.stream()
            .filter(mth -> "generatedIn".equals(mth.getSimpleName().toString())).findFirst().get();
        generatedClassVisibilityConfigMethod = hktMethods.stream()
            .filter(mth -> "withVisibility".equals(mth.getSimpleName().toString())).findFirst().get();
        generatedMethodTemplateConfigMethod = hktMethods.stream()
            .filter(mth -> "methodNames".equals(mth.getSimpleName().toString())).findFirst().get();
        generatedMethodDelegationConfigMethod = hktMethods.stream()
            .filter(mth -> "delegateTo".equals(mth.getSimpleName().toString())).findFirst().get();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {

            final Stream<TypeElement> allTypes = ElementFilter
                .typesIn(roundEnv.getRootElements())
                .parallelStream()
                .flatMap(tel -> Stream.concat(Stream.of(tel), allInnerTypes(tel)));

            final Stream<HktToValidate> targetTypes = allTypes
                .flatMap(this::asHktToValidate);

            Function<HkTypeError, Consumer<TypeElement>> reportErrors = reportErrors();

            Stream<Runnable> errorReportIo = targetTypes
                .flatMap(hktToValidate -> hktToValidate.match((typeConstructor, hktInterface) ->
                    checkHktType(typeConstructor, hktInterface)
                        .map(reportErrors)
                        .map(c -> () -> c.accept(typeConstructor))
                ));

            errorReportIo.forEach(Runnable::run);
        }
        return false;
    }

    private static Optional<JavaCompiler.JdkSpecificApi> jdkSpecificApi(ProcessingEnvironment processingEnv) {
        return processingEnv.getElementUtils().getTypeElement("com.sun.source.util.Trees") != null
               ? Optional.of(new OpenJdkSpecificApi(processingEnv))
               : Optional.empty();
    }

    private Stream<TypeElement> allInnerTypes(TypeElement tel) {
        final Stream<TypeElement> memberTypes =
            ElementFilter.typesIn(tel.getEnclosedElements()).stream();

        final Stream<TypeElement> localTypes = JdkSpecificApi.map(jdkSpecificApi -> jdkSpecificApi.localTypes(tel))
            .orElse(Stream.empty());

        final List<TypeElement> allTypes =
            Stream.concat(memberTypes, localTypes).collect(Collectors.toList());

        return allTypes.isEmpty()
               ? Stream.empty()
               : Stream.concat
                   (allTypes.stream(), allTypes.parallelStream().flatMap(this::allInnerTypes));
    }

    private Stream<HktToValidate> asHktToValidate(TypeElement tEl) {
        return tEl.getInterfaces().stream()
            .map(this::asHktInterface)
            .flatMap(Opt::asStream)
            .limit(1)
            .map(hktInterface -> HktToValidates.of(tEl, hktInterface));
    }

    private Stream<HkTypeError> checkHktType(TypeElement typeConstructor, DeclaredType hktInterface) {
        return Stream.of(
            checkHktInterfaceNotRawType(hktInterface),
            checkAtLeastOneTypeParameter(typeConstructor),
            checkRightHktInterface(typeConstructor, hktInterface),
            checkTypeParameters(typeConstructor, hktInterface),
            checkTCWitness(typeConstructor, hktInterface),
            checkNestedTCWitnessHasNoTypeParameter(typeConstructor, hktInterface),
            checkNestedTCWitnessIsStaticFinal(typeConstructor, hktInterface)
        ).flatMap(Opt::asStream);
    }

    private Optional<HkTypeError> checkHktInterfaceNotRawType(DeclaredType hktInterface) {
        return hktInterface.getTypeArguments().isEmpty()
               ? Optional.of(HKTInterfaceDeclrationIsRawType())
               : Optional.empty();
    }

    private Optional<HkTypeError> checkAtLeastOneTypeParameter(TypeElement typeConstructor) {
        return typeConstructor.getTypeParameters().isEmpty()
               ? Optional.of(HKTypesNeedAtLeastOneTypeParameter())
               : Optional.empty();
    }

    private Optional<HkTypeError> checkRightHktInterface(TypeElement typeConstructor, DeclaredType hktInterface) {
        return asTypeElement.visit(hktInterface.asElement())
            .filter(hktInterfaceElement -> typeConstructor.getTypeParameters().size() + 1 != hktInterfaceElement.getTypeParameters().size())
            .map(__ -> WrongHKTInterface());
    }

    private Optional<HkTypeError> checkTypeParameters(TypeElement typeConstructor, DeclaredType hktInterface) {

        List<? extends TypeParameterElement> typeParameters = typeConstructor.getTypeParameters();
        List<? extends TypeMirror> typeArguments = hktInterface.getTypeArguments();

        List<TypeParameterElement> typeParamsInError = IntStream.range(0, min(typeParameters.size(), typeArguments.size() - 1))
            .filter(i -> !Types.isSameType(typeParameters.get(i).asType(), typeArguments.get(i + 1)))
            .mapToObj(typeParameters::get)
            .collect(Collectors.toList());

        return typeParamsInError.isEmpty()
               ? Optional.empty()
               : Optional.of(NotMatchingTypeParams(typeParamsInError));
    }

    private Optional<HkTypeError> checkTCWitness(TypeElement typeConstructor, DeclaredType hktInterface) {

        return hktInterface.getTypeArguments().stream().findFirst()
            .flatMap(witnessTm -> asValidTCWitness(typeConstructor, witnessTm).isPresent()
                                  ? Optional.empty()
                                  : Optional.of(TCWitnessMustBeNestedClassOrClass())
            );
    }

    private Optional<DeclaredType> asValidTCWitness(TypeElement typeConstructor, TypeMirror witnessTm) {
        return asDeclaredType.visit(witnessTm)
            .filter(witness ->
                Types.isSameType(witness, Types.erasure(typeConstructor.asType()))
                    || Types.isSameType(witness, Types.getDeclaredType(typeConstructor, typeConstructor.getTypeParameters().stream()
                    .map(__ -> Types.getWildcardType(null, null))
                    .toArray(TypeMirror[]::new)))
                    || witness.asElement().getEnclosingElement().equals(typeConstructor)
            );
    }

    private Optional<HkTypeError> checkNestedTCWitnessHasNoTypeParameter(TypeElement typeConstructor, DeclaredType hktInterface) {

        return hktInterface.getTypeArguments().stream().findFirst()
            .flatMap(witnessTm ->
                asDeclaredType.visit(witnessTm).map(DeclaredType::asElement).flatMap(asTypeElement::visit)
                    .filter(witness -> witness.getEnclosingElement().equals(typeConstructor))
                    .flatMap(witness -> witness.getTypeParameters().isEmpty()
                                        ? Optional.empty()
                                        : Optional.of(NestedTCWitnessMustBeSimpleType(witness))
                    )
            );
    }

    private Optional<HkTypeError> checkNestedTCWitnessIsStaticFinal(TypeElement typeConstructor, DeclaredType hktInterface) {
        return hktInterface.getTypeArguments().stream().findFirst()
            .flatMap(witnessTm ->
                asDeclaredType.visit(witnessTm).map(DeclaredType::asElement).flatMap(asTypeElement::visit)
                    .filter(witness -> witness.getEnclosingElement().equals(typeConstructor))
                    .flatMap(witness -> (witness.getKind() == ElementKind.INTERFACE ||  witness.getModifiers().contains(Modifier.STATIC))
                                            && (!typeConstructor.getModifiers().contains(Modifier.PUBLIC) || typeConstructor.getKind() == ElementKind.INTERFACE || witness.getModifiers().contains(Modifier.PUBLIC))
                                        ? Optional.empty()
                                        : Optional.of(NestedTCWitnessMustBeStaticFinal(witness))
                    )
            );
    }


    private Optional<DeclaredType> asHktInterface(TypeMirror tm) {
        return asDeclaredType.visit(tm)
            .filter(declaredType ->  Elts.getPackageOf(declaredType.asElement()).equals(Elts.getPackageOf(__Elt)))
            .filter(declaredType -> Types.isSubtype(declaredType, Types.erasure(__Elt.asType())));
    }

    private Function<HkTypeError, Consumer<TypeElement>> reportErrors() {
        return HkTypeErrors.cases()
            .<Consumer<TypeElement>>HKTInterfaceDeclrationIsRawType(
                (typeElement) -> Messager.printMessage(Diagnostic.Kind.ERROR, hKTInterfaceDeclrationIsRawTypeErrorMessage(typeElement), typeElement))

            .HKTypesNeedAtLeastOneTypeParameter(
                (typeElement) -> Messager.printMessage(Diagnostic.Kind.ERROR, hKTypesNeedAtLeastOneTypeParameterErrorMessage(typeElement), typeElement))

            .WrongHKTInterface(
                (typeElement) -> Messager.printMessage(Diagnostic.Kind.ERROR, wrongHKTInterfaceErrorMessage(typeElement), typeElement))

            .NotMatchingTypeParams(typeParameterElements ->
                (typeElement) -> typeParameterElements.stream().forEach(typeParameterElement ->
                    Messager.printMessage(Diagnostic.Kind.ERROR, notMatchingTypeParamErrorMessage(typeElement), typeParameterElement)))

            .TCWitnessMustBeNestedClassOrClass(
                (typeElement) -> Messager.printMessage(Diagnostic.Kind.ERROR, tcWitnessMustBeNestedClassOrClassErrorMessage(typeElement), typeElement))

            .NestedTCWitnessMustBeSimpleType(tcWitnessElement ->
                (typeElement) -> Messager.printMessage(Diagnostic.Kind.ERROR, nestedTCWitnessMustBeSimpleTypeErrorMessage(), tcWitnessElement))

            .NestedTCWitnessMustBeStaticFinal(tcWitnessElement ->
                (typeElement) -> Messager.printMessage(Diagnostic.Kind.ERROR, nestedTCWitnessMustBePublicStaticErrorMessage(typeElement), tcWitnessElement));
    }

    private String hKTInterfaceDeclrationIsRawTypeErrorMessage(TypeElement tel) {
        return format("%s interface declaration is missing type arguments:%n%s",
            implementedHktInterfaceName(tel),
            expectedHktInterfaceMessage(tel));
    }

    private String hKTypesNeedAtLeastOneTypeParameterErrorMessage(TypeElement tel) {
        return format("%s need at least one type parameter to correctly implement %s",
            tel.toString(), implementedHktInterfaceName(tel));
    }

    private String wrongHKTInterfaceErrorMessage(TypeElement tel) {
        return format("%s is not the correct interface to use.%nGiven the number of type parameters, %s",
            implementedHktInterfaceName(tel), expectedHktInterfaceMessage(tel));
    }

    private String notMatchingTypeParamErrorMessage(TypeElement tel) {
        return format("The type parameters of %s must appear in the same order in the declaration of %s:%n%s",
            tel.toString(), implementedHktInterfaceName(tel), expectedHktInterfaceMessage(tel));
    }

    private String tcWitnessMustBeNestedClassOrClassErrorMessage(TypeElement tel) {
        return format("Type constructor witness (first type argument of %s) is incorrect:%n%s",
            implementedHktInterfaceName(tel), expectedHktInterfaceMessage(tel));
    }

    private String nestedTCWitnessMustBeSimpleTypeErrorMessage() {
        return "The nested class used as type constructor witness must not take any type parameter";
    }

    private String nestedTCWitnessMustBePublicStaticErrorMessage(TypeElement tel) {
        return format("The nested class used as type constructor witness must be '%sstatic final'.",
            tel.getModifiers().contains(Modifier.PUBLIC) ? "public " : "");
    }

    private String implementedHktInterfaceName(TypeElement tel) {
        return tel.getInterfaces().stream().map(this::asHktInterface).flatMap(Opt::asStream).findFirst()
            .map(DeclaredType::asElement).map(Element::toString).orElse("");
    }

    private String expectedHktInterfaceMessage(TypeElement tel) {
        return format("%s should %s %s", tel.toString(), tel.getKind() == ElementKind.CLASS ? "implements" : "extends",

            Opt.cata(tel.getInterfaces().stream()
                    .map(this::asHktInterface)
                    .flatMap(Opt::asStream)
                    .map(hktInterface -> hktInterface.getTypeArguments().stream()
                        .findFirst().flatMap(tm -> asValidTCWitness(tel, tm)))
                    .flatMap(Opt::asStream)
                    .findFirst(),

                tcWitness -> expectedHktInterface(tel, tcWitness.toString()),

                () -> tel.getTypeParameters().size() <= 1
                      ? expectedHktInterface(tel, Types.getDeclaredType(tel, tel.getTypeParameters().stream()
                    .map(__ -> Types.getWildcardType(null, null))
                    .toArray(TypeMirror[]::new)).toString())
                      : format("%s with %s being the following nested class of %s:%n    %s",
                          expectedHktInterface(tel, µTypeName), µTypeName, tel.toString(), "public static final class µ {}")

            ));
    }

    private String expectedHktInterface(TypeElement tel, String witness) {
        int nbTypeParameters = tel.getTypeParameters().size();
        return format("%s%s<%s, %s>", __Elt.getQualifiedName().toString(),
            nbTypeParameters <= 1 ? "" : String.valueOf(nbTypeParameters),
            witness,
            tel.getTypeParameters().stream().map(Element::asType).map(TypeMirror::toString)
                .reduce((tp1, tp2) -> tp1 + ", " + tp2).orElse("")
        );
    }


    private GenCodeConf genCodeConf(Element element) {

        Function<GenCodeConf, DataTypes.GenCodeConf> codeGenConfigOverride = Function.identity();

        for (Element e = element; e != null; e = element.getEnclosingElement()) {

            Map<? extends ExecutableElement, ? extends AnnotationValue> overridenAttributes = e.getAnnotationMirrors().stream()
                .filter(am -> HktAnnot.equals(am.getAnnotationType().asElement()))
                .map(AnnotationMirror::getElementValues)
                .findFirst().orElse(Collections.emptyMap());

            AnnotationValue classNameOverride = overridenAttributes.get(generatedClassNameConfigMethod);
            if (classNameOverride != null) {
                codeGenConfigOverride = setClassName((String) classNameOverride.getValue()).andThen(codeGenConfigOverride);
            }

            AnnotationValue visibilityOverride = overridenAttributes.get(generatedClassVisibilityConfigMethod);
            if (visibilityOverride != null) {
                codeGenConfigOverride = setVisibility((Hkt.Visibility) visibilityOverride.getValue()).andThen(codeGenConfigOverride);
            }

            AnnotationValue methodTemplateOverride = overridenAttributes.get(generatedMethodTemplateConfigMethod);
            if (methodTemplateOverride != null) {
                codeGenConfigOverride =  setCoerceMethodTemplate((String) methodTemplateOverride.getValue()).andThen(codeGenConfigOverride);
            }

            AnnotationValue delegationOverride = overridenAttributes.get(generatedMethodDelegationConfigMethod);
            if (delegationOverride != null) {
                EnumSet<Hkt.Generator> generators = EnumSet.noneOf(Hkt.Generator.class);
                generators.addAll(Arrays.asList((Hkt.Generator[]) delegationOverride.getValue()));
                codeGenConfigOverride =  setCodeGenerator(unmodifiableSet(generators)).andThen(codeGenConfigOverride);
            }

        }

        return codeGenConfigOverride.apply(GenCodeConf.defaultConfig);
    }

    private static final TypeVisitor<Optional<DeclaredType>, Unit> asDeclaredType =
        new SimpleTypeVisitor8<Optional<DeclaredType>, Unit>(Optional.empty()) {
            @Override
            public Optional<DeclaredType> visitDeclared(final DeclaredType t, final Unit p) {
                return Optional.of(t);
            }
        };

    private static final ElementVisitor<Optional<TypeElement>, Unit> asTypeElement =
        new SimpleElementVisitor8<Optional<TypeElement>, Unit>(Optional.empty()) {
            @Override
            public Optional<TypeElement> visitType(final TypeElement e, final Unit p) {
                return Optional.of(e);
            }
        };

}

