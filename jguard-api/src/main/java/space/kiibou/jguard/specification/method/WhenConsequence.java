package space.kiibou.jguard.specification.method;

import space.kiibou.jguard.Guard;

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
