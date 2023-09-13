package space.kiibou.byteguard.agent;

import space.kiibou.byteguard.annotation.SpecFor;
import space.kiibou.byteguard.specification.GuardSpec;
import space.kiibou.byteguard.specification.method.MethodSpec;

@SpecFor("space.kiibou.byteguard.agent.PredicateGuardedMethods")
class PredicateGuardedMethodsSpecs extends GuardSpec {

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
