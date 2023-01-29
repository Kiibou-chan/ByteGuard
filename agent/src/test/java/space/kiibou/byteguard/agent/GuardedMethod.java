package space.kiibou.byteguard.agent;

import space.kiibou.byteguard.Guard;
import space.kiibou.byteguard.annotation.SpecFor;
import space.kiibou.byteguard.specification.GuardSpec;
import space.kiibou.byteguard.specification.method.MethodSpec;

public class GuardedMethod {

    public void toggleMethod() {
    }

    public void method() {
    }

    @SpecFor("space.kiibou.byteguard.agent.GuardedMethod")
    static class GuardedMethodSpec extends GuardSpec {

        public Guard canCallMethod = guard();

        public MethodSpec toggleMethod() {
            return methodSpec(
                    when(canCallMethod.isReset()).then(set(canCallMethod)).orElse(reset(canCallMethod))
            );
        }

        public MethodSpec method() {
            return methodSpec(
                    requires(canCallMethod.isSet())
            );
        }

    }

}
