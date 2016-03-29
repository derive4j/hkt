package dummy.dumb;

import org.derive4j.hkt.__;

public class Bar {

    static class InnerClass<T> implements __<Bar, Void> {
        public static final class µ {}
    }

    static class StaticInnerClass {
        StaticInnerClass() {
//            class InConstructorClass implements __<Bar, Void> {
//                public static final class µ {}
//            }
        }

        void method() {
            class InMethodClass {

                void subMethod() {
                    //class InSubMethodClass implements __<Bar, Void> {}
                }
            }
        }
    }
}

class CompanionClass {}
