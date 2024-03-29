package space.kiibou.byteguard.bytecode;

import space.kiibou.byteguard.annotation.SpecFor;
import space.kiibou.byteguard.specification.GuardSpec;
import space.kiibou.byteguard.specification.method.MethodSpec;

public class PredicateGuardedMethods {

    public void method() {
    }

    public String method(String str) {
        return str;
    }

    @SpecFor("space.kiibou.byteguard.bytecode.PredicateGuardedMethods")
    static class PredicateGuardedMethodsSpec extends GuardSpec {

        public MethodSpec method() {
            return methodSpec(
                    requires(() -> false)
            );
        }

        public MethodSpec method(String str) {
            return methodSpec(
                    requires(() -> str.startsWith("a"))
            );
        }

    }

}
