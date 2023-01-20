package space.kiibou.jguard.bytecode;

import space.kiibou.jguard.Guard;
import space.kiibou.jguard.annotation.SpecFor;
import space.kiibou.jguard.specification.GuardSpec;
import space.kiibou.jguard.specification.method.MethodSpec;

public class GuardedMethod {

    public void toggleMethod() {
    }

    public void method() {

    }

    @SpecFor("space.kiibou.jguard.bytecode.GuardedMethod")
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
