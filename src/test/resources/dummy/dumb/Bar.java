package dummy.dumb;

import org.derive4j.hkt.__;
import org.derive4j.hkt.__2;

public class Bar {

  static class InnerClass<T, S, U, L, P> implements __<Bar, Void> {
    public static final class µ {}
  }

  static class StaticInnerClass {
    StaticInnerClass() {
      class InConstructorClass implements __<Bar, Void> {
        final class µ {}
      }
    }

    void method() {
      class InMethodClass {

        void subMethod() {
          class InSubMethodClass implements __<Bar, Void> {}
        }
      }
    }
  }
}

class CompanionClass<A> implements __2<Void, Void, Void> {}
