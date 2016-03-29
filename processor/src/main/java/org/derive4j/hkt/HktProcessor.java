package org.derive4j.hkt;

import com.google.auto.service.AutoService;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import org.derive4j.Data;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleTypeVisitor8;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("*")
public final class HktProcessor extends AbstractProcessor {
    private static final String  µTypeName = "µ";

    private Filer Filer;
    private Types Types;
    private Elements Elts;
    private Messager Messager;
    private Trees JTrees;

    private TypeElement __Elt;
    private TypeElement __2Elt;
    private TypeElement __3Elt;
    private TypeElement __4Elt;
    private TypeElement __5Elt;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        Filer = processingEnv.getFiler();
        Types = processingEnv.getTypeUtils();
        Elts = processingEnv.getElementUtils();
        Messager = processingEnv.getMessager();
        JTrees = Trees.instance(processingEnv);

        __Elt = Elts.getTypeElement(__.class.getCanonicalName());
        __2Elt = Elts.getTypeElement(__2.class.getCanonicalName());
        __3Elt = Elts.getTypeElement(__3.class.getCanonicalName());
        __4Elt = Elts.getTypeElement(__4.class.getCanonicalName());
        __5Elt = Elts.getTypeElement(__5.class.getCanonicalName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {

            final Stream<TypeElement> allTypes = ElementFilter
                .typesIn(roundEnv.getRootElements())
                .parallelStream()
                .flatMap(tel -> Stream
                    .concat(Stream.of(tel), getAllInnerTypes(tel)));

            final Stream<TypeElement> targetTypes = allTypes.filter(this::implementsHkt);

            final Stream<Repr> reprs = targetTypes.map(tel -> cata(getInnerµ(tel)
                , µ -> Reprs.Hasµ(tel, getHktDecl(tel).get(), µ) // Optionnal.get safe since we previously discarded types that don't implement Hkt (__ or __2 or etc...)
                , () -> Reprs.Noµ(tel)));

            final Stream<Report> reports = reprs.map(Reprs.cases()
                .Noµ(Reports::Noµ)
                .Hasµ(this::validateRepr));

            reports.forEach(this::reportErrors);
        }
        return true;
    }

    private Stream<TypeElement> getAllInnerTypes(TypeElement tel) {
        final List<? extends Element> enclosedElements = tel.getEnclosedElements();

        final Stream<TypeElement> memberTypes =
            ElementFilter.typesIn(enclosedElements).stream();

        final Stream<TypeElement> localTypes = Stream
            .concat(ElementFilter.constructorsIn(enclosedElements).stream()
                , ElementFilter.methodsIn(enclosedElements).stream())
            .flatMap(exEl -> {
                final MethodTree methodTree = JTrees.getTree(exEl);
                final TreePath methodPath = JTrees.getPath(exEl);
                final List<TypeElement> typeElts = ElementFilter.typesIn(methodTree
                    .getBody()
                    .getStatements()
                    .stream()
                    .filter(st -> st.accept(new ClassTreeVisitor(), Unit.unit))
                    .map(st -> JTrees.getElement(TreePath.getPath(methodPath, st)))
                    .collect(Collectors.toList()));
                return typeElts.stream();
            });

        final List<TypeElement> allTypes =
            Stream.concat(memberTypes, localTypes).collect(Collectors.toList());

        return allTypes.isEmpty()
            ? Stream.empty()
            : Stream.concat
            (allTypes.stream(), allTypes.parallelStream().flatMap(this::getAllInnerTypes));
    }

    private Optional<Mu> getInnerµ(TypeElement elt) {
        return ElementFilter
            .typesIn(elt.getEnclosedElements())
            .stream()
            .filter(HktProcessor::isµ)
            .findAny()
            .flatMap(tel -> elt
                .asType()
                .accept(new DeclaredTypeVisitor(), Unit.unit)
                .map(decl -> Mus.of(tel, Types.asMemberOf(decl, tel))));
    }

    private boolean implementsHkt(TypeElement tel) {
        return Types.isSubtype(tel.asType(), Types.erasure(__Elt.asType()));
    }

    private Optional<DeclaredType> getHktDecl(TypeElement tel) {
        return
        tel.getInterfaces()
            .stream()
            .filter(this::isHkt)
            .findAny()
            .flatMap(tm -> tm.accept(new DeclaredTypeVisitor(), Unit.unit));
    }

    private boolean isHkt(TypeMirror tm) {
        final Function<TypeMirror, Boolean> isSameType =
            curry(Types::isSameType).apply(Types.erasure(tm));

        return isSameType.apply(Types.erasure(__Elt.asType()))
            || isSameType.apply(Types.erasure(__2Elt.asType()))
            || isSameType.apply(Types.erasure(__3Elt.asType()))
            || isSameType.apply(Types.erasure(__4Elt.asType()))
            || isSameType.apply(Types.erasure(__5Elt.asType()));
    }

    private Report validateRepr(TypeElement elt, DeclaredType decl, Mu µ) {
        final TypeMirror[] eltParams =
            elt.getTypeParameters()
                .stream()
                .map(Element::asType)
                .toArray(TypeMirror[]::new);

        final TypeElement refType;
        switch (eltParams.length) {
            case 1 : refType = __Elt; break;
            case 2 : refType = __2Elt; break;
            case 3 : refType = __3Elt; break;
            case 4 : refType = __4Elt; break;
            case 5 : refType = __5Elt; break;
            default: throw new Error
                (elt.getQualifiedName() + " : at least one and at most 5 parameters must be declared");
        }

        final DeclaredType refDecl =
            Types.getDeclaredType(refType, Stream
                .concat(Stream.of(Mus.getAsMirror(µ)), Stream.of(eltParams))
                .toArray(TypeMirror[]::new));

        return Types.isSameType(refDecl, decl)
            ? Reports.Correct()
            : Reports.WrongImpl(elt, refDecl);
    }

    private Unit reportErrors(Report report) {
        return Reports.cases()
            .Correct(() -> Unit.unit)
            .Noµ(tel -> {
                Messager.printMessage(Diagnostic.Kind.ERROR, noµError(tel), tel);
                return Unit.unit;
            })
            .WrongImpl((tel, refDecl) -> {
                Messager.printMessage(Diagnostic.Kind.ERROR, wrongImplError(tel, refDecl), tel);
                return Unit.unit;
            })
            .apply(report);
    }

    private static boolean isµ(TypeElement elt) {
        return
            elt.getSimpleName().contentEquals(µTypeName)
            && elt.getModifiers().containsAll(asList
                (Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL));
    }

    private static String noµError(TypeElement tel) {
        return String.format("%n%s must declare an inner public static final class called 'µ'", tel.getQualifiedName());
    }

    private static String wrongImplError(TypeElement tel, DeclaredType refDecl) {
        return String
            .format("%n%s takes %d type parameter(s). To be declared as a type constructor,%n"
                + "it can only implements :%n%n%s%n%n(plus any interfaces outside org.derive4j.hkt)"
                , tel.getQualifiedName()
                , tel.getTypeParameters().size()
                , refDecl);
    }

    private static final class DeclaredTypeVisitor extends SimpleTypeVisitor8<Optional<DeclaredType>, Unit> {
        DeclaredTypeVisitor() {}
        @Override
        public Optional<DeclaredType> visitDeclared(DeclaredType t, Unit __) {
            return Optional.of(t);
        }
        @Override
        protected Optional<DeclaredType> defaultAction(TypeMirror e, Unit __) {
            return Optional.empty();
        }
    }

    private static final class ClassTreeVisitor extends SimpleTreeVisitor<Boolean, Unit> {
        ClassTreeVisitor() {}
        @Override
        public Boolean visitClass(ClassTree node, Unit __) { return true; }
        @Override
        protected Boolean defaultAction(Tree node, Unit __) { return false; }
    }

    private static <T, R> R cata(Optional<T> opt, Function<T, R> f, Supplier<R> r) {
        return opt.map(f).orElseGet(r);
    }

    private static <A, B, C> Function<A, Function<B, C>> curry(BiFunction<A, B, C> f) {
        return a -> b -> f.apply(a, b);
    }

    private enum Unit { unit }

    @Data
    static abstract class Repr {
        interface Cases<R> {
            R Noµ(TypeElement elt);
            R Hasµ(TypeElement asElt, DeclaredType asDecl, Mu µ);
        }
        abstract <R> R match(Cases<R> cases);
    }

    @Data
    static abstract class Mu {
        interface Cases<R> {
            R of(TypeElement asElt, TypeMirror asMirror);
        }
        abstract <R> R match(Cases<R> cases);
    }

    @Data
    static abstract class Report {
        interface Cases<R> {
            R Correct();
            R Noµ(TypeElement elt);
            R WrongImpl(TypeElement elt, DeclaredType refDecl);
        }
        abstract <R> R match(Cases<R> cases);
    }
}
