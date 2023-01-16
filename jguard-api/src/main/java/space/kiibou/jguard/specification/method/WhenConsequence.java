package space.kiibou.jguard.specification.method;

import space.kiibou.jguard.Guard;

public sealed interface WhenConsequence {

    record SetsGuard(Guard guard) implements WhenConsequence {
    }

    record ResetsGuard(Guard guard) implements WhenConsequence {
    }

    static WhenConsequence setsGuard(final Guard guard) {
        return new SetsGuard(guard);
    }

    static WhenConsequence resetsGuard(final Guard guard) {
        return new ResetsGuard(guard);
    }

}
