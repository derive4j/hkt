package org.derive4j.hkt.processor;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.StatementTree;
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

import static org.derive4j.hkt.processor.DataTypes.Opt.unNull;
import static org.derive4j.hkt.processor.DataTypes.Unit.unit;

final class JavaCompiler {
    private JavaCompiler() {}

    interface JdkSpecificApi {
        Stream<TypeElement> localTypes(TypeElement tel);
    }

    static final class OpenJdkSpecificApi implements JdkSpecificApi {
        private final Trees JTrees;
        OpenJdkSpecificApi(ProcessingEnvironment processingEnv) {
            JTrees = Trees.instance(processingEnv);
        }

        @Override
        public Stream<TypeElement> localTypes(TypeElement tel) {
            final List<? extends Element> enclosedElements = tel.getEnclosedElements();

            return Stream
                .concat(ElementFilter.constructorsIn(enclosedElements).stream()
                    , ElementFilter.methodsIn(enclosedElements).stream())
                .flatMap(exEl -> unNull(JTrees.getTree(exEl))

                    .flatMap(methodTree -> unNull(JTrees.getPath(exEl))
                        .flatMap(methodPath -> unNull(methodTree.getBody())
                            .flatMap(methodBody -> unNull(methodBody.getStatements())
                                .map(statements -> {
                                    final List<TypeElement> typeElts = ElementFilter.typesIn(statements
                                        .stream()
                                        .filter(OpenJdkSpecificApi::isClassDecl)
                                        .flatMap(cdl -> Opt
                                            .asStream(unNull(TreePath.getPath(methodPath, cdl))
                                                .flatMap(path -> unNull(JTrees.getElement(path)))))
                                        .collect(Collectors.toList()));

                                    return typeElts.stream();
                                }))))

                    .orElseGet(Stream::empty));
        }

        private static <T extends StatementTree > Boolean isClassDecl(T st) {
            return st.accept(ClassTreeVisitor.self, unit);
        }

        private static final class ClassTreeVisitor extends SimpleTreeVisitor<Boolean, Unit> {
            ClassTreeVisitor() {}

            static final ClassTreeVisitor self = new ClassTreeVisitor();

            @Override
            public Boolean visitClass(ClassTree node, Unit __) { return true; }
            @Override
            protected Boolean defaultAction(Tree node, Unit __) { return false; }
        }
    }
}
