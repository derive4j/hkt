package org.derive4j.hkt;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configuration annotation for the generation of safe coerce methods from the higher kinded type encoding to the actual type.
 * The annotation is overridable by methods: package and class annotated deeper in the package hierarchy override the configuration
 * found in annotated packages closer to the root.
 */
@Target({ElementType.PACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface HktConfig {

  /**
   * All coerce methods for a given package will be generated in a class with that name.
   */
  String generatedIn() default "Hkt";

  enum Visibility {
    Same,
    Package
  }

  /**
   * Define the visibility of the generated class: same as the higher kinded class or package.
   */
  Visibility withVisibility() default Visibility.Package;

  /**
   * Template for the name of the generated methods that handle safe coercion from a {@link __} instance to the actual type.
   * Should contains {@code {ClassName}} in reference to the class of the type.
   */
  String methodNames() default "as{ClassName}";

  /**
   * May be used to enforce the name of the witness type (see {@link __}) throughout a project or a package
   */
  String witnessTypeName() default "µ";

  enum Generator {
    derive4j;
  }

  /**
   * A list of generators that should handle generation of the coerce method instead of the hkt processor,
   * if a given class is annotated for that processor.
   */
  Generator[] delegateTo() default {Generator.derive4j};

}
