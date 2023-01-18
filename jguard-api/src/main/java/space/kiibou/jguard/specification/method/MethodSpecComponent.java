package space.kiibou.jguard.specification.method;

import space.kiibou.jguard.specification.GuardState;

public sealed interface MethodSpecComponent {

    record RequiresGuardState(GuardState guardState) implements MethodSpecComponent {
    }

    sealed interface WhenComponent extends MethodSpecComponent {
        WhenCondition condition();
    }

    record WhenThenConsequence(WhenCondition condition, WhenConsequence consequence) implements WhenComponent {
        public WhenThenElseConsequence orElse(WhenConsequence elseConsequence) {
            return new WhenThenElseConsequence(condition, consequence, elseConsequence);
        }
    }

    record WhenThenElseConsequence(WhenCondition condition, WhenConsequence thenConsequence, WhenConsequence elseConsequence) implements WhenComponent {
    }

    static MethodSpecComponent requiresGuardState(final GuardState guardState) {
        return new RequiresGuardState(guardState);
    }

    static MethodSpecComponent whenReturnsThen(ReturnsPredicate predicate, WhenConsequence consequence) {
        return new WhenThenConsequence(new WhenCondition.Returns(predicate), consequence);
    }

    static MethodSpecComponent whenGuardConditionThen(GuardState state, WhenConsequence consequence) {
        return new WhenThenConsequence(new WhenCondition.GuardCondition(state), consequence);
    }

    static MethodSpecComponent whenGuardConditionThenElse(GuardState state, WhenConsequence thenConsequence, WhenConsequence elseConsequence) {
        return new WhenThenElseConsequence(new WhenCondition.GuardCondition(state), thenConsequence, elseConsequence);
    }

}
