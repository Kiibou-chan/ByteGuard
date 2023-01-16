package space.kiibou.jguard.test;

import space.kiibou.jguard.Guard;
import space.kiibou.jguard.annotation.SpecFor;
import space.kiibou.jguard.specification.GuardSpec;
import space.kiibou.jguard.specification.method.MethodSpec;

@SpecFor("space.kiibou.jguard.test.IteratorWrapper")
public final class IteratorGuardSpec extends GuardSpec {

    public final Guard canCallNext = guard(Guard.State.RESET);

    public MethodSpec hasNext() {
        return methodSpec(
                when(returns(true), sets(canCallNext))
        );
    }

    public MethodSpec next() {
        return methodSpec(
                requires(canCallNext.isSet()),
                when(returns(), resets(canCallNext))
        );
    }

}
