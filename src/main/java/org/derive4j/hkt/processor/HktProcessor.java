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
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleElementVisitor8;
import javax.lang.model.util.SimpleTypeVisitor8;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import org.derive4j.Data;
import org.derive4j.hkt.Hkt;
import org.derive4j.hkt.__;

import static java.lang.Math.min;
import static java.lang.String.format;
import static java.util.Collections.unmodifiableSet;
import static org.derive4j.hkt.processor.CodeGenConfigs.Config;
import static org.derive4j.hkt.processor.CodeGenConfigs.setCassName;
import static org.derive4j.hkt.processor.CodeGenConfigs.setCoerceMethodTemplate;
import static org.derive4j.hkt.processor.CodeGenConfigs.setVisibility;
import static org.derive4j.hkt.processor.HkTypeErrors.HKTInterfaceDeclrationIsRawType;
import static org.derive4j.hkt.processor.HkTypeErrors.HKTypesNeedAtLeastOneTypeParameter;
import static org.derive4j.hkt.processor.HkTypeErrors.NestedTCWitnessMustBeSimpleType;
import static org.derive4j.hkt.processor.HkTypeErrors.NestedTCWitnessMustBeStaticFinal;
import static org.derive4j.hkt.processor.HkTypeErrors.NotMatchingTypeParams;
import static org.derive4j.hkt.processor.HkTypeErrors.TCWitnessMustBeNestedClassOrClass;
import static org.derive4j.hkt.processor.HkTypeErrors.WrongHKTInterface;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("*")
public final class HktProcessor extends AbstractProcessor {

    private static final String  µTypeName = "µ";

    private Filer Filer;
    private Types Types;
    private Elements Elts;
    private Messager Messager;
    private Optional<HktProcessor.JdkSpecificApi> JdkSpecificApi;

    private TypeElement __Elt;

    private TypeElement HktElt;

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

        HktElt = Elts.getTypeElement(Hkt.class.getCanonicalName());
        List<ExecutableElement> hktMethods = ElementFilter.methodsIn(HktElt.getEnclosedElements());
        this.generatedClassNameConfigMethod = hktMethods.stream()
            .filter(executableElement -> "generatedIn".equals(executableElement.getSimpleName().toString())).findFirst().get();
        this.generatedClassVisibilityConfigMethod = hktMethods.stream()
            .filter(executableElement -> "withVisibility".equals(executableElement.getSimpleName().toString())).findFirst().get();
        this.generatedMethodTemplateConfigMethod = hktMethods.stream()
            .filter(executableElement -> "methodNames".equals(executableElement.getSimpleName().toString())).findFirst().get();
        this.generatedMethodDelegationConfigMethod = hktMethods.stream()
            .filter(executableElement -> "delegateTo".equals(executableElement.getSimpleName().toString())).findFirst().get();
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

    private interface JdkSpecificApi {
        Stream<TypeElement> localTypes(TypeElement tel);
    }

    private static final class OpenJdkSpecificApi implements JdkSpecificApi {
        private Trees JTrees;
        OpenJdkSpecificApi(ProcessingEnvironment processingEnv) {
            JTrees = Trees.instance(processingEnv);
        }

        @Override public Stream<TypeElement> localTypes(TypeElement tel) {
            final List<? extends Element> enclosedElements = tel.getEnclosedElements();

            return Stream.concat
                (ElementFilter.constructorsIn(enclosedElements).stream()
                    , ElementFilter.methodsIn(enclosedElements).stream()).flatMap
                (exEl -> unNull(JTrees.getTree(exEl)).flatMap
                    (methodTree -> unNull(JTrees.getPath(exEl)).flatMap
                        (methodPath -> unNull(methodTree.getBody()).flatMap
                            (methodBody -> unNull(methodBody.getStatements()).map
                                (statements -> {
                                    final List<TypeElement> typeElts = ElementFilter.typesIn(statements
                                        .stream()
                                        .filter(st -> st.accept(new ClassTreeVisitor(), Unit.unit))
                                        .map(st ->
                                            unNull(TreePath.getPath(methodPath, st)).flatMap
                                                (path -> unNull(JTrees.getElement(path))))
                                        .filter(Optional::isPresent)
                                        .map(Optional::get)
                                        .collect(Collectors.toList()));
                                    return typeElts.stream();
                                }))))
                    .orElseGet(Stream::empty));
        }
    }

    private static Optional<JdkSpecificApi> jdkSpecificApi(ProcessingEnvironment processingEnv) {
        return processingEnv.getElementUtils().getTypeElement("com.sun.source.util.Trees") != null
               ? Optional.of(new OpenJdkSpecificApi(processingEnv))
               : Optional.empty();
    }

    private Stream<TypeElement> allInnerTypes(TypeElement tel) {
        final List<? extends Element> enclosedElements = tel.getEnclosedElements();

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
            .flatMap(HktProcessor::optionalAsStream)
            .limit(1)
            .map(hktInterface -> HktToValidates.of(tEl, hktInterface));
    }

    @Data
    static abstract class HkTypeError {
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

    private Stream<HkTypeError> checkHktType(TypeElement typeConstructor, DeclaredType hktInterface) {
        return Stream.of(
            checkHktInterfaceNotRawType(hktInterface),
            checkAtLeastOneTypeParameter(typeConstructor),
            checkRightHktInterface(typeConstructor, hktInterface),
            checkTypeParameters(typeConstructor, hktInterface),
            checkTCWitness(typeConstructor, hktInterface),
            checkNestedTCWitnessHasNoTypeParameter(typeConstructor, hktInterface),
            checkNestedTCWitnessIsStaticFinal(typeConstructor, hktInterface)
        ).flatMap(HktProcessor::optionalAsStream);
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
        return tel.getInterfaces().stream().map(this::asHktInterface).flatMap(HktProcessor::optionalAsStream).findFirst()
            .map(DeclaredType::asElement).map(Element::toString).orElse("");
    }

    private String expectedHktInterfaceMessage(TypeElement tel) {
        return format("%s should %s %s", tel.toString(), tel.getKind() == ElementKind.CLASS ? "implements" : "extends",

            cata(tel.getInterfaces().stream()
                    .map(this::asHktInterface)
                    .flatMap(HktProcessor::optionalAsStream)
                    .map(hktInterface -> hktInterface.getTypeArguments().stream()
                        .findFirst().flatMap(tm -> asValidTCWitness(tel, tm)))
                    .flatMap(HktProcessor::optionalAsStream)
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


    @Data
    static abstract class CodeGenConfig {
        interface Case<R> {
            R Config(String cassName, Hkt.Visibility visibility, String coerceMethodTemplate, Set<Hkt.Generator> codeGenerator);
        }
        abstract <R> R match(Case<R> Config);

        static final CodeGenConfig defaultConfig = Config("Hkt", Hkt.Visibility.Same, "as{ClassName}", unmodifiableSet(EnumSet.of(Hkt.Generator.derive4j)));
    }

    private CodeGenConfig codeGenConfig(Element element) {

        Function<CodeGenConfig, CodeGenConfig> codeGenConfigOverride = Function.identity();

        for (Element e = element; e != null; e = element.getEnclosingElement()) {

            Map<? extends ExecutableElement, ? extends AnnotationValue> overridenAttributes = e.getAnnotationMirrors().stream()
                .filter(am -> HktElt.equals(am.getAnnotationType().asElement()))
                .map(AnnotationMirror::getElementValues)
                .findFirst().orElse(Collections.emptyMap());

            AnnotationValue classNameOverride = overridenAttributes.get(generatedClassNameConfigMethod);
            if (classNameOverride != null) {
                codeGenConfigOverride = setCassName((String) classNameOverride.getValue()).andThen(codeGenConfigOverride);
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
                codeGenConfigOverride = CodeGenConfigs.setCodeGenerator(unmodifiableSet(generators)).andThen(codeGenConfigOverride);
            }

        }

        return codeGenConfigOverride.apply(CodeGenConfig.defaultConfig);
    }

    private static final class ClassTreeVisitor extends SimpleTreeVisitor<Boolean, Unit> {
        ClassTreeVisitor() {}
        @Override
        public Boolean visitClass(ClassTree node, Unit __) { return true; }
        @Override
        protected Boolean defaultAction(Tree node, Unit __) { return false; }
    }

    private static <T> Optional<T> unNull(T t) { return Optional.ofNullable(t); }

    private static <K, V> Optional<V> safeGet(K key, Map<? super K, V> map) { return unNull(map.get(key)); }

    private static <T, R> R cata(Optional<T> opt, Function<T, R> f, Supplier<R> r) {
        return opt.map(f).orElseGet(r);
    }

    private static <A, B, C> Function<A, Function<B, C>> curry(BiFunction<A, B, C> f) {
        return a -> b -> f.apply(a, b);
    }

    private enum Unit { unit }

    @Data
    static abstract class HktToValidate {
        interface Cases<R> {
            R of(TypeElement typeConstructor, DeclaredType hktInterface);
        }
        abstract <R> R match(Cases<R> cases);
    }

    private static final TypeVisitor<Optional<DeclaredType>, Unit> asDeclaredType = new SimpleTypeVisitor8<Optional<DeclaredType>, Unit>(
        Optional.empty()) {
        @Override
        public Optional<DeclaredType> visitDeclared(final DeclaredType t, final Unit p) {
            return Optional.of(t);
        }
    };

    private static final ElementVisitor<Optional<TypeElement>, Unit> asTypeElement = new SimpleElementVisitor8<Optional<TypeElement>, Unit>(Optional.empty()) {

        @Override
        public Optional<TypeElement> visitType(final TypeElement e, final Unit p) {
            return Optional.of(e);
        }

    };

    private static <A> Stream<A> optionalAsStream(Optional<A> oa) {
        return cata(oa, Stream::of, Stream::empty);
    }

}

