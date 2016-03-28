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
    private TypeElement hktType;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        Filer = processingEnv.getFiler();
        Types = processingEnv.getTypeUtils();
        Elts = processingEnv.getElementUtils();
        Messager = processingEnv.getMessager();
        JTrees = Trees.instance(processingEnv);
        hktType = Elts.getTypeElement(__.class.getCanonicalName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {

            final Stream<TypeElement> allTypes = ElementFilter
                .typesIn(roundEnv.getRootElements())
                .stream()
                .flatMap(tel -> Stream
                    .concat(Stream.of(tel), getAllInnerTypes(tel)));

            final Stream<TypeElement> targetTypes = allTypes.filter(this::implementsHkt);

            final Stream<Repr> reprs = targetTypes.map(tel -> {
                final DeclaredType decl =
                    getHktDecl(tel).get(); // safe since we previously discarded types that don't implement Hkt (__)

                return cata(getInnerµ(tel)
                    , µ -> Reprs.Hasµ(tel, decl, µ)
                    , () -> Reprs.Noµ(tel));
            });

            final Stream<Report> reports = reprs.map(Reprs.cases()
                .Noµ(Reports::Noµ)
                .Hasµ(this::correctHtkImpl));

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
            : Stream
            .concat(allTypes.stream()
                , allTypes.stream().flatMap(this::getAllInnerTypes));
    }

    private boolean implementsHkt(TypeElement tel) {
        return Types.isSubtype(tel.asType(), Types.erasure(hktType.asType()));
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
        return Types.isSameType(Types.erasure(tm), Types.erasure(hktType.asType()));
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

    private Report correctHtkImpl(TypeElement elt, DeclaredType decl, Mu µ) {
        return cata(decl.getTypeArguments().stream().findFirst()

            , tm -> Types.isSameType(tm, Mus.getAsMirror(µ))
                ? Reports.Correct()
                : isHkt(tm)
                ? correctHtkImpl(elt, tm.accept(new DeclaredTypeVisitor(), Unit.unit).get(), µ)
                : Reports.WrongImpl(elt)

            , () -> Reports.WrongImpl(elt));
    }

    private Unit reportErrors(Report report) {
        return Reports.cases()
            .Correct(() -> Unit.unit)
            .Noµ(tel -> {
                Messager.printMessage(Diagnostic.Kind.ERROR, noµError(tel), tel);
                return Unit.unit;
            })
            .WrongImpl(tel -> {
                Messager.printMessage(Diagnostic.Kind.ERROR, wrongImplError(tel), tel);
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
        return "\n" + tel.getQualifiedName() + " must declare an inner public static final class called 'µ'";
    }

    private static String wrongImplError(TypeElement tel) {
        return "\nThe first parameter of a " + __.class.getCanonicalName() + " implementation declaration must be either " +
            tel.getQualifiedName() + ".µ or " + __.class.getCanonicalName();
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
            R WrongImpl(TypeElement elt);
        }
        abstract <R> R match(Cases<R> cases);
    }
}
