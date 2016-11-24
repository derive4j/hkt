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
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import org.derive4j.hkt.HktConfig;
import org.derive4j.hkt.__;
import org.derive4j.hkt.processor.DataTypes.HkTypeError;
import org.derive4j.hkt.processor.DataTypes.HktConf;
import org.derive4j.hkt.processor.DataTypes.HktDecl;
import org.derive4j.hkt.processor.DataTypes.IO;
import org.derive4j.hkt.processor.DataTypes.Opt;
import org.derive4j.hkt.processor.DataTypes.P2;
import org.derive4j.hkt.processor.DataTypes.Unit;
import org.derive4j.hkt.processor.DataTypes.Valid;
import org.derive4j.hkt.processor.JavaCompiler.OpenJdkSpecificApi;

import static java.lang.Math.min;
import static java.lang.String.format;
import static org.derive4j.hkt.processor.DataTypes.Opt.unNull;
import static org.derive4j.hkt.processor._HkTypeError.HKTInterfaceDeclIsRawType;
import static org.derive4j.hkt.processor._HkTypeError.HKTypesNeedAtLeastOneTypeParameter;
import static org.derive4j.hkt.processor._HkTypeError.NestedTCWitnessMustBeSimpleType;
import static org.derive4j.hkt.processor._HkTypeError.NestedTCWitnessMustBeStaticFinal;
import static org.derive4j.hkt.processor._HkTypeError.NotMatchingTypeParams;
import static org.derive4j.hkt.processor._HkTypeError.TCWitnessMustBeNestedClassOrClass;
import static org.derive4j.hkt.processor._HkTypeError.WrongHKTInterface;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("*")
public final class HktProcessor extends AbstractProcessor {

    private Types Types;
    private Elements Elts;
    private Messager Messager;
    private GenCode GenCode;
    private Optional<JavaCompiler.JdkSpecificApi> JdkSpecificApi;

    private TypeElement __Elt;

    private TypeElement HktConfigElt;
    private ExecutableElement witnessTypeNameConfMethod;
    private ExecutableElement generateInConfMethod;
    private ExecutableElement withVisibilityConfMethod;
    private ExecutableElement coerceMethodNameConfMethod;
    private ExecutableElement typeEqMethodNameConfMethod;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        Types = processingEnv.getTypeUtils();
        Elts = processingEnv.getElementUtils();
        Messager = processingEnv.getMessager();
        JdkSpecificApi = jdkSpecificApi(processingEnv);

        __Elt = Elts.getTypeElement(__.class.getCanonicalName());
        GenCode = new GenCode(Elts, Types, processingEnv.getFiler(), __Elt);

        HktConfigElt = Elts.getTypeElement(HktConfig.class.getName());
        witnessTypeNameConfMethod = unsafeGetExecutableElement(HktConfigElt, "witnessTypeName");
        generateInConfMethod = unsafeGetExecutableElement(HktConfigElt, "generateIn");
        withVisibilityConfMethod = unsafeGetExecutableElement(HktConfigElt, "withVisibility");
        coerceMethodNameConfMethod = unsafeGetExecutableElement(HktConfigElt, "coerceMethodName");
        typeEqMethodNameConfMethod = unsafeGetExecutableElement(HktConfigElt, "typeEqMethodName");
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        final Stream<TypeElement> allTypes = ElementFilter
            .typesIn(roundEnv.getRootElements())
            .parallelStream()
            .flatMap(tel -> Stream.concat(Stream.of(tel), allInnerTypes(tel)));

        final Stream<HktDecl> targetTypes = allTypes.map(this::asHktDecl).flatMap(Opt::asStream);

        final Stream<Valid<List<HkTypeError>>> validations = targetTypes.map(this::checkHktType);

        P2<List<HktDecl>, List<P2<HktDecl, List<HkTypeError>>>> successFailures = Valid.partition(validations);

        Map<String, List<HktDecl>> hktDeclByGenClassName = successFailures._1().stream()
            .collect(Collectors.groupingBy(GenCode::genClassName));

        Stream<IO<Unit>> generationActions = hktDeclByGenClassName.entrySet()
            .stream()
            .map(e -> GenCode.run(e.getKey(), e.getValue()));

        final IO<Unit> effects = IO.sequenceStream_(Stream.concat
            (generationActions, successFailures._2().stream().map(p -> p.match(this::reportErrors))));

        effects.runUnchecked();

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

        final Stream<TypeElement> localTypes =
            JdkSpecificApi.map(jdkSpecificApi -> jdkSpecificApi.localTypes(tel)).orElse(Stream.empty());

        final List<TypeElement> allTypes =
            Stream.concat(memberTypes, localTypes).collect(Collectors.toList());

        return allTypes.isEmpty()
            ? Stream.empty()
            : Stream.concat
                (allTypes.stream(), allTypes.stream().flatMap(this::allInnerTypes));
    }

    private Optional<HktDecl> asHktDecl(TypeElement tEl) {
        return findImplementedHktInterface(tEl.asType()).map(hktInterface -> _HktDecl.of(tEl, hktInterface, hktConf(tEl)));
    }

    private Valid<List<HkTypeError>> checkHktType(HktDecl hktDecl) {
        return Valid.accumulate(hktDecl, Stream.of(
            checkHktInterfaceNotRawType(hktDecl),
            checkAtLeastOneTypeParameter(hktDecl),
            checkRightHktInterface(hktDecl),
            checkTypeParameters(hktDecl),
            checkTCWitness(hktDecl),
            checkNestedTCWitnessHasNoTypeParameter(hktDecl),
            checkNestedTCWitnessIsStaticFinal(hktDecl)
        ));
    }

    private <E> Optional<E> check(boolean property, E otherWiseError) {
        return property ? Optional.empty() : Optional.of(otherWiseError);
    }

    private Optional<HkTypeError> checkHktInterfaceNotRawType(HktDecl hktDecl) {
        return check(!_HktDecl.getHktInterface(hktDecl).getTypeArguments().isEmpty(),  HKTInterfaceDeclIsRawType());
    }


    private Optional<HkTypeError> checkAtLeastOneTypeParameter(HktDecl hktDecl) {
        return check(!_HktDecl.getTypeConstructor(hktDecl).getTypeParameters().isEmpty(), HKTypesNeedAtLeastOneTypeParameter());
    }

    private Optional<HkTypeError> checkRightHktInterface(HktDecl hktDecl) {
        return Visitors.asTypeElement.visit(_HktDecl.getHktInterface(hktDecl).asElement())
            .filter(hktInterfaceElement ->
                _HktDecl.getTypeConstructor(hktDecl).getTypeParameters().size() + 1
                    != hktInterfaceElement.getTypeParameters().size())
            .map(__ -> WrongHKTInterface());
    }

    private Optional<HkTypeError> checkTypeParameters(HktDecl hktDecl) {
        final List<? extends TypeParameterElement> typeParameters =
            _HktDecl.getTypeConstructor(hktDecl).getTypeParameters();
        final List<? extends TypeMirror> typeArguments =
            _HktDecl.getHktInterface(hktDecl).getTypeArguments();

        List<TypeParameterElement> typeParamsInError = IntStream
            .range(0, min(typeParameters.size(), typeArguments.size() - 1))
            .filter(i -> !Types.isSameType(typeParameters.get(i).asType(), typeArguments.get(i + 1)))
            .mapToObj(typeParameters::get)
            .collect(Collectors.toList());

        return check(typeParamsInError.isEmpty(), NotMatchingTypeParams(typeParamsInError));
    }

    private Optional<HkTypeError> checkTCWitness(HktDecl hktDecl) {
        return check(_HktDecl
            .getHktInterface(hktDecl).getTypeArguments().stream().findFirst()
            .flatMap(witnessTm -> asValidTCWitness(_HktDecl.getTypeConstructor(hktDecl), witnessTm))
            .isPresent(), TCWitnessMustBeNestedClassOrClass());
    }
    private Optional<DeclaredType> asValidTCWitness(TypeElement typeConstructor, TypeMirror witnessTm) {
        return Visitors.asDeclaredType.visit(witnessTm)
            .filter(witness ->
                Types.isSameType(witness, Types.erasure(typeConstructor.asType()))
                    || Types.isSameType(witness, Types.getDeclaredType(typeConstructor, typeConstructor.getTypeParameters().stream()
                    .map(__ -> Types.getWildcardType(null, null))
                    .toArray(TypeMirror[]::new)))
                    || witness.asElement().getEnclosingElement().equals(typeConstructor));
    }

    private Optional<HkTypeError> checkNestedTCWitnessHasNoTypeParameter(HktDecl hktDecl) {
        return _HktDecl
            .getHktInterface(hktDecl).getTypeArguments().stream().findFirst()
            .flatMap(witnessTm ->
                Visitors.asDeclaredType.visit(witnessTm).map(DeclaredType::asElement).flatMap(Visitors.asTypeElement::visit)
                    .filter(witness -> witness.getEnclosingElement().equals(_HktDecl.getTypeConstructor(hktDecl)))
                    .flatMap(witness -> witness.getTypeParameters().isEmpty()
                        ? Optional.empty()
                        : Optional.of(NestedTCWitnessMustBeSimpleType(witness))));
    }

    private Optional<HkTypeError> checkNestedTCWitnessIsStaticFinal(HktDecl hktDecl) {
        final TypeElement typeConstructor = _HktDecl.getTypeConstructor(hktDecl);

        return _HktDecl
            .getHktInterface(hktDecl).getTypeArguments().stream().findFirst()
            .flatMap(witnessTm ->
                Visitors.asDeclaredType.visit(witnessTm).map(DeclaredType::asElement).flatMap(Visitors.asTypeElement::visit)
                    .filter(witness -> witness.getEnclosingElement().equals(typeConstructor))
                    .flatMap(witness -> (witness.getKind() == ElementKind.INTERFACE ||  witness.getModifiers().contains(Modifier.STATIC))
                        && (!typeConstructor.getModifiers().contains(Modifier.PUBLIC) || typeConstructor.getKind() == ElementKind.INTERFACE || witness.getModifiers().contains(Modifier.PUBLIC))
                        ? Optional.empty()
                        : Optional.of(NestedTCWitnessMustBeStaticFinal(witness))));
    }

    private Optional<DeclaredType> findImplementedHktInterface(TypeMirror type) {
        return allSuperTypes(type).map(this::asHktInterface).flatMap(Opt::asStream).findFirst()
            .filter(hktInterface ->
                    allSuperTypes(type).noneMatch(s -> !Types.isSubtype(hktInterface, s)
                        && findImplementedHktInterface(s.asElement().asType()).isPresent()));
    }

    private Stream<DeclaredType> allSuperTypes(TypeMirror typeMirror) {
        return Visitors.allSuperTypes(Types, typeMirror);
    }

    private Optional<DeclaredType> asHktInterface(TypeMirror tm) {
        return Visitors.asDeclaredType.visit(tm)
            .filter(declaredType -> Elts.getPackageOf(declaredType.asElement()).equals(Elts.getPackageOf(__Elt)))
            .filter(declaredType -> Types.isSubtype(declaredType, Types.erasure(__Elt.asType())))
            .filter(declaredType -> !declaredType.getTypeArguments()
                .stream()
                .allMatch(typeArg -> typeArg.getKind() == TypeKind.TYPEVAR));
    }

    private IO<Unit> reportErrors(HktDecl hktDecl, List<HkTypeError> errors) {
        final TypeElement typeElement = _HktDecl.getTypeConstructor(hktDecl);
        final HktConf conf = _HktDecl.getConf(hktDecl);

        final Stream<IO<Unit>> effects = errors.stream().map(_HkTypeError.cases()
            .HKTInterfaceDeclIsRawType(IO.effect(() -> Messager.printMessage
                (Diagnostic.Kind.ERROR, hKTInterfaceDeclIsRawTypeErrorMessage(typeElement, conf), typeElement)))

            .HKTypesNeedAtLeastOneTypeParameter(IO.effect(() -> Messager.printMessage
                (Diagnostic.Kind.ERROR, hKTypesNeedAtLeastOneTypeParameterErrorMessage(typeElement), typeElement)))

            .WrongHKTInterface(IO.effect(() -> Messager.printMessage
                (Diagnostic.Kind.ERROR, wrongHKTInterfaceErrorMessage(typeElement, conf), typeElement)))

            .NotMatchingTypeParams(typeParameterElements -> IO.effect(() ->
                typeParameterElements.forEach(typeParameterElement -> Messager.printMessage
                    (Diagnostic.Kind.ERROR, notMatchingTypeParamErrorMessage(typeElement, conf), typeParameterElement))))

            .TCWitnessMustBeNestedClassOrClass(IO.effect(() -> Messager.printMessage
                (Diagnostic.Kind.ERROR, tcWitnessMustBeNestedClassOrClassErrorMessage(typeElement, conf), typeElement)))

            .NestedTCWitnessMustBeSimpleType(tcWitnessElement -> IO.effect(() -> Messager.printMessage
                (Diagnostic.Kind.ERROR, nestedTCWitnessMustBeSimpleTypeErrorMessage(), tcWitnessElement)))

            .NestedTCWitnessMustBeStaticFinal(tcWitnessElement -> IO.effect(() -> Messager.printMessage
                (Diagnostic.Kind.ERROR, nestedTCWitnessMustBePublicStaticErrorMessage(typeElement), tcWitnessElement))));

        return IO.sequenceStream_(effects);
    }

    private String hKTInterfaceDeclIsRawTypeErrorMessage(TypeElement tel, HktConf conf) {
        return format("%s interface declaration is missing type arguments:%n%s",
            implementedHktInterfaceName(tel),
            expectedHktInterfaceMessage(tel, conf));
    }

    private String hKTypesNeedAtLeastOneTypeParameterErrorMessage(TypeElement tel) {
        return format("%s need at least one type parameter to correctly implement %s",
            tel.toString(), implementedHktInterfaceName(tel));
    }

    private String wrongHKTInterfaceErrorMessage(TypeElement tel, HktConf conf) {
        return format("%s is not the correct interface to use.%nGiven the number of type parameters, %s",
            implementedHktInterfaceName(tel), expectedHktInterfaceMessage(tel, conf));
    }

    private String notMatchingTypeParamErrorMessage(TypeElement tel, HktConf conf) {
        return format("The type parameters of %s must appear in the same order in the declaration of %s:%n%s",
            tel.toString(), implementedHktInterfaceName(tel), expectedHktInterfaceMessage(tel, conf));
    }

    private String tcWitnessMustBeNestedClassOrClassErrorMessage(TypeElement tel, HktConf conf) {
        return format("Type constructor witness (first type argument of %s) is incorrect:%n%s",
            implementedHktInterfaceName(tel), expectedHktInterfaceMessage(tel, conf));
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

    private String expectedHktInterfaceMessage(TypeElement tel, HktConf conf) {
        final String witnessTypeName = Optional.of(_HktConf.getWitnessTypeName(conf)).filter(w -> !w.startsWith(":")).orElse("µ");

        return format("%s should %s %s", tel.toString(), tel.getKind() == ElementKind.CLASS ? "implements" : "extends",

            Opt.cata(findImplementedHktInterface(tel.asType())
                    .flatMap(hktInterface -> hktInterface.getTypeArguments().stream()
                        .findFirst().flatMap(tm -> asValidTCWitness(tel, tm)))

                , tcWitness -> expectedHktInterface(tel, tcWitness.toString())

                , () -> tel.getTypeParameters().size() <= 1

                    ? expectedHktInterface(tel, Types.getDeclaredType(tel, tel.getTypeParameters().stream()
                    .map(__ -> Types.getWildcardType(null, null))
                    .toArray(TypeMirror[]::new)).toString())

                    : format("%s with %s being the following nested class of %s:%n    %s"
                    , expectedHktInterface(tel, witnessTypeName)
                    , witnessTypeName
                    , tel.toString()
                    , "public enum " + witnessTypeName + " {}")));
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

    private HktConf hktConf(Element elt) {
        return hktConfDefaultMod(elt).apply(HktConf.defaultConfig);
    }

    private Function<HktConf, HktConf> hktConfDefaultMod(Element elt) {
        Function<HktConf, HktConf> conf = elt.getAnnotationMirrors()
            .stream()
            .filter(am -> am.getAnnotationType().asElement().equals(this.HktConfigElt))
            .map(am -> {
                Map<? extends ExecutableElement, ? extends AnnotationValue> explicitValues = am.getElementValues();

                Optional<Function<HktConf, HktConf>> witnessTypeName = unNull(explicitValues.get(witnessTypeNameConfMethod)).map(
                    value -> _HktConf.setWitnessTypeName((String) Visitors.getAnnotationValue.visit(value)));

                Optional<Function<HktConf, HktConf>> generateIn = unNull(explicitValues.get(generateInConfMethod)).map(
                    value -> _HktConf.setClassName((String) Visitors.getAnnotationValue.visit(value)));

                Optional<Function<HktConf, HktConf>> withVisibility = unNull(explicitValues.get(withVisibilityConfMethod)).map(
                    value -> _HktConf.setVisibility(
                        HktConfig.Visibility.valueOf((String) Visitors.getAnnotationValue.visit(value))));

                Optional<Function<HktConf, HktConf>> coerceMethodName = unNull(
                    explicitValues.get(coerceMethodNameConfMethod)).map(
                    value -> _HktConf.setCoerceMethodTemplate((String) Visitors.getAnnotationValue.visit(value)));

                Optional<Function<HktConf, HktConf>> typeEqMethodName = unNull(
                    explicitValues.get(typeEqMethodNameConfMethod)).map(
                    value -> _HktConf.setTypeEqMethodTemplate((String) Visitors.getAnnotationValue.visit(value)));

                return Stream.of(witnessTypeName, generateIn, withVisibility, coerceMethodName, typeEqMethodName)
                    .flatMap(Opt::asStream)
                    .reduce(Function::andThen)
                    .orElse(Function.identity());

            })
            .findAny()
            .orElse(Function.identity());

        return Opt.cata(parentElt(elt),
            parentElt -> conf.compose(hktConfDefaultMod(parentElt)),
            () -> conf);
    }

    private Optional<Element> parentElt(Element elt) {
        return Opt.cata(unNull(elt.getEnclosingElement())
            , Optional::of
            , () -> parentPkg((PackageElement) elt));
    }

    private Optional<Element> parentPkg(PackageElement elt) {
        int lastDot = elt.getQualifiedName().toString().lastIndexOf('.');
        return (lastDot == -1)
            ? Optional.empty()
            : unNull(Elts.getPackageElement(elt.getQualifiedName().subSequence(0, lastDot)));
    }

    private static ExecutableElement unsafeGetExecutableElement(TypeElement typeElement, String methodName) {
        return (ExecutableElement) typeElement.getEnclosedElements()
            .stream()
            .filter(e -> e.getSimpleName().contentEquals(methodName))
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException(typeElement + "#" + methodName));
    }

}

