package space.kiibou.byteguard.bytecode;

import space.kiibou.byteguard.annotation.SpecFor;
import space.kiibou.byteguard.specification.GuardSpec;
import space.kiibou.byteguard.specification.method.MethodSpec;

public class InstanceCapturingReqPred {

    private final String value;
    private final String prefix;

    public InstanceCapturingReqPred(String value, String prefix) {
        this.value = value;
        this.prefix = prefix;
    }

    public String unit() {
        return value;
    }

    public String concat(String other) {
        return value + other;
    }

    public String concat(String s1, String s2, String s3) {
        return value + s1 + s2 + s3;
    }

    @SpecFor("space.kiibou.byteguard.bytecode.InstanceCapturingReqPred")
    public static class InstanceCapturingReqPredSpec extends GuardSpec {

        String value;
        String prefix;

        public MethodSpec unit() {
            return methodSpec(
                    requires(() -> value.startsWith(prefix))
            );
        }

        public MethodSpec concat(String other) {
            return methodSpec(
                    requires(() -> value.startsWith(prefix)),
                    requires(() -> other.startsWith(prefix))
            );
        }

        public MethodSpec concat(String s1, String s2, String s3) {
            return methodSpec(
                    requires(() -> value.startsWith(prefix)),
                    requires(() -> s1.startsWith(prefix)),
                    requires(() -> s2.startsWith(prefix)),
                    requires(() -> s3.startsWith(prefix))
            );
        }

    }

}
