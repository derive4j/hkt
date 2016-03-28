package dummy.dumb;

public class Bar {

    class InnerClass {}

    static class StaticInnerClass {
        StaticInnerClass() {
            class InConstructorClass {}
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
