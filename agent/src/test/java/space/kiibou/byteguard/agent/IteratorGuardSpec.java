package space.kiibou.byteguard.agent;

import space.kiibou.byteguard.Guard;
import space.kiibou.byteguard.annotation.SpecFor;
import space.kiibou.byteguard.specification.GuardSpec;
import space.kiibou.byteguard.specification.method.MethodSpec;

@SpecFor("space.kiibou.byteguard.agent.IteratorWrapper")
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
