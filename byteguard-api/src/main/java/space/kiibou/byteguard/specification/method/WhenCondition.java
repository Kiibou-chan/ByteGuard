package space.kiibou.byteguard.specification.method;

import space.kiibou.byteguard.specification.GuardState;

public sealed interface WhenCondition {

    default MethodSpecComponent.WhenThenConsequence then(WhenConsequence thenConsequence) {
        return new MethodSpecComponent.WhenThenConsequence(this, thenConsequence);
    }

    record Returns(ReturnsPredicate predicate) implements WhenCondition {
    }

    record GuardCondition(GuardState guardState) implements WhenCondition {
    }

}
