package space.kiibou.jguard.bytecode;

import space.kiibou.jguard.Guard;
import space.kiibou.jguard.annotation.SpecFor;
import space.kiibou.jguard.specification.GuardSpec;
import space.kiibou.jguard.specification.method.MethodSpec;

@SpecFor("space.kiibou.jguard.testing.IteratorWrapper")
public final class IteratorGuardSpec extends GuardSpec {

    public final Guard canCallNext = guard(Guard.State.RESET);

    public MethodSpec hasNext() {
        return methodSpec(
                when(returns(true)).then(set(canCallNext))
        );
    }

    public MethodSpec next() {
        return methodSpec(
                requires(canCallNext.isSet()),
                when(returns()).then(reset(canCallNext))
        );
    }

}
