package space.kiibou.byteguard.specification.method;

import space.kiibou.byteguard.Guard;

public sealed interface WhenConsequence {

    record SetsGuard(Guard guard) implements WhenConsequence {
    }

    record ResetsGuard(Guard guard) implements WhenConsequence {
    }

    static WhenConsequence setGuard(final Guard guard) {
        return new SetsGuard(guard);
    }

    static WhenConsequence resetGuard(final Guard guard) {
        return new ResetsGuard(guard);
    }

}
