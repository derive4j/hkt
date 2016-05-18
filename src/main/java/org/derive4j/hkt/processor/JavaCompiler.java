package org.derive4j.hkt.processor;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import org.derive4j.hkt.processor.DataTypes.Opt;
import org.derive4j.hkt.processor.DataTypes.Unit;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.derive4j.hkt.processor.DataTypes.Unit.unit;

final class JavaCompiler {
    private JavaCompiler() {}

    interface JdkSpecificApi {
        Stream<TypeElement> localTypes(TypeElement tel);
    }

    static final class OpenJdkSpecificApi implements JdkSpecificApi {
        private Trees JTrees;
        OpenJdkSpecificApi(ProcessingEnvironment processingEnv) {
            JTrees = Trees.instance(processingEnv);
        }

        @Override public Stream<TypeElement> localTypes(TypeElement tel) {
            final List<? extends Element> enclosedElements = tel.getEnclosedElements();

            return Stream.concat
                (ElementFilter.constructorsIn(enclosedElements).stream()
                    , ElementFilter.methodsIn(enclosedElements).stream()).flatMap
                (exEl -> Opt.unNull(JTrees.getTree(exEl)).flatMap
                    (methodTree -> Opt.unNull(JTrees.getPath(exEl)).flatMap
                        (methodPath -> Opt.unNull(methodTree.getBody()).flatMap
                            (methodBody -> Opt.unNull(methodBody.getStatements()).map
                                (statements -> {
                                    final List<TypeElement> typeElts = ElementFilter.typesIn(statements
                                        .stream()
                                        .filter(st -> st.accept(new ClassTreeVisitor(), unit))
                                        .flatMap(st ->
                                            Opt.asStream(Opt.unNull(TreePath.getPath(methodPath, st)).flatMap
                                                (path -> Opt.unNull(JTrees.getElement(path)))))
                                        .collect(Collectors.toList()));
                                    return typeElts.stream();
                                }))))
                    .orElseGet(Stream::empty));
        }

        private static final class ClassTreeVisitor extends SimpleTreeVisitor<Boolean, Unit> {
            ClassTreeVisitor() {}
            @Override
            public Boolean visitClass(ClassTree node, Unit __) { return true; }
            @Override
            protected Boolean defaultAction(Tree node, Unit __) { return false; }
        }
    }
}
