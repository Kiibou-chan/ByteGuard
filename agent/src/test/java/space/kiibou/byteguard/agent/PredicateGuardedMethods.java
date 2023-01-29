package space.kiibou.byteguard.agent;

import space.kiibou.byteguard.specification.GuardSpec;
import space.kiibou.byteguard.specification.method.MethodSpec;

public class PredicateGuardedMethods {

    public void method() {
    }

    public String method(String str) {
        return str;
    }

    // FIXME (Svenja, 2023/01/29): For some reason, Specs which contain Lambdas and are inner classes of their target
    //  class somehow hinder the correct loading of the changed target class. Having the Spec in a different class
    //  (see PredicateGuardedMethodsSpec.java) works. Why does that happen? How can we circumvent the problem?
    // @SpecFor("space.kiibou.byteguard.agent.PredicateGuardedMethods")
    static class PredicateGuardedMethodsSpec extends GuardSpec {

        public MethodSpec method() {
            return methodSpec(
                    requires(() -> {
                        System.out.println("test1");
                        return false;
                    })
            );
        }

        public MethodSpec method(String str) {
            return methodSpec(
                    requires(() -> {
                        System.out.println("test2");
                        return str.startsWith("a");
                    })
            );
        }

    }
}
