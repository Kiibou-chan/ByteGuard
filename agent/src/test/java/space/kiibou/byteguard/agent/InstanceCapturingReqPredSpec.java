package space.kiibou.byteguard.agent;

import space.kiibou.byteguard.annotation.SpecFor;
import space.kiibou.byteguard.specification.GuardSpec;
import space.kiibou.byteguard.specification.method.MethodSpec;

@SpecFor("space.kiibou.byteguard.agent.InstanceCapturingReqPred")
public class InstanceCapturingReqPredSpec extends GuardSpec {

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
