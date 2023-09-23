package space.kiibou.byteguard.agent;

import space.kiibou.byteguard.annotation.SpecFor;
import space.kiibou.byteguard.specification.GuardSpec;
import space.kiibou.byteguard.specification.method.MethodSpec;

public class PredicateGuardedMethods {

    public void method() {
    }

    public String method(String str) {
        return str;
    }

    // FIXME (Svenja, 2023/01/29): For some reason, this spec can not be used as an inner class... I don't know why
    // @SpecFor("space.kiibou.byteguard.agent.PredicateGuardedMethods")
    public static class PredicateGuardedMethodsSpec extends GuardSpec {

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
