package org.derive4j.hkt.typechecker;

import com.google.common.truth.Truth;
import com.google.testing.compile.JavaFileObjects;
import org.junit.Ignore;
import org.junit.Test;

import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;

public class HktProcessorTest {

  @Test
  public void bad_encodings() {
    Truth.assert_()
        .about(javaSource())
        .that(JavaFileObjects.forResource("dummy/dumb/Bar.java"))
        .processedWith(new HktProcessor())
        .failsToCompile()
        .withErrorCount(6);
  }
}
