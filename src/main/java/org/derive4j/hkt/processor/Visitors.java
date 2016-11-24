package org.derive4j.hkt.processor;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.AnnotationValueVisitor;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.util.AbstractAnnotationValueVisitor8;
import javax.lang.model.util.SimpleElementVisitor8;
import javax.lang.model.util.SimpleTypeVisitor8;
import javax.lang.model.util.Types;
import org.derive4j.hkt.processor.DataTypes.Unit;

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

  static Stream<DeclaredType> allSuperTypes(Types types, TypeMirror typeMirror) {
    return types.directSupertypes(typeMirror)
        .stream()
        .map(Visitors.asDeclaredType::visit)
        .flatMap(DataTypes.Opt::asStream)
        .flatMap(s -> Stream.concat(Stream.of(s), allSuperTypes(types, s)));
  }

  static final AnnotationValueVisitor<Object, Void> getAnnotationValue = new AbstractAnnotationValueVisitor8<Object, Void>() {
    @Override
    public Object visitBoolean(boolean b, Void aVoid) {
      return b;
    }

    @Override
    public Object visitByte(byte b, Void aVoid) {
      return b;
    }

    @Override
    public Object visitChar(char c, Void aVoid) {
      return c;
    }

    @Override
    public Object visitDouble(double d, Void aVoid) {
      return d;
    }

    @Override
    public Object visitFloat(float f, Void aVoid) {
      return f;
    }

    @Override
    public Object visitInt(int i, Void aVoid) {
      return i;
    }

    @Override
    public Object visitLong(long i, Void aVoid) {
      return i;
    }

    @Override
    public Object visitShort(short s, Void aVoid) {
      return s;
    }

    @Override
    public Object visitString(String s, Void aVoid) {
      return s;
    }

    @Override
    public Object visitType(TypeMirror t, Void aVoid) {
      return asDeclaredType.visit(t)
          .flatMap(dt -> asTypeElement.visit(dt.asElement()))
          .orElseThrow(IllegalArgumentException::new);
    }

    @Override
    public Object visitEnumConstant(VariableElement c, Void aVoid) {
      return c.getSimpleName().toString();
    }

    @Override
    public Object visitAnnotation(AnnotationMirror a, Void aVoid) {
      return a.getElementValues();
    }

    @Override
    public Object visitArray(List<? extends AnnotationValue> vals, Void aVoid) {
      return vals.stream().map(this::visit).collect(Collectors.toList());
    }
  };
}
