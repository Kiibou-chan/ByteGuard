package space.kiibou.byteguard.bytecode;

import space.kiibou.byteguard.annotation.SpecFor;
import space.kiibou.byteguard.specification.GuardSpec;
import space.kiibou.byteguard.specification.method.MethodSpec;

public class MultipleReqPredicates {

    public String concat(String a, Double b, Integer c) {
        return a + b + c;
    }

    @SpecFor("space.kiibou.byteguard.bytecode.MultipleReqPredicates")
    public static class MultipleReqPredicatesSpec extends GuardSpec {

        public MethodSpec concat(String a, Double b, Integer c) {
            return methodSpec(
                    requires(() -> a != null),
                    requires(() -> b != null),
                    requires(() -> c != null)
            );
        }

    }

}
