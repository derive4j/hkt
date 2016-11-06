package org.derive4j.hkt.processor;

import org.derive4j.hkt.processor.DataTypes.Unit;

import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.util.SimpleElementVisitor8;
import javax.lang.model.util.SimpleTypeVisitor8;
import java.util.Optional;

final class Visitors {
    private Visitors() {}

    static final TypeVisitor<Optional<DeclaredType>, Unit> asDeclaredType =
        new SimpleTypeVisitor8<Optional<DeclaredType>, Unit>(Optional.empty()) {
            @Override
            public Optional<DeclaredType> visitDeclared(final DeclaredType t, final Unit p) {
                return Optional.of(t);
            }
        };

    static final ElementVisitor<Optional<TypeElement>, Unit> asTypeElement =
        new SimpleElementVisitor8<Optional<TypeElement>, Unit>(Optional.empty()) {
            @Override
            public Optional<TypeElement> visitType(final TypeElement e, final Unit p) {
                return Optional.of(e);
            }
        };
}
