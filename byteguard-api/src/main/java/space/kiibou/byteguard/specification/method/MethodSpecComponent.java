package space.kiibou.byteguard.specification.method;

import space.kiibou.byteguard.specification.GuardState;

import java.util.function.BooleanSupplier;

public sealed interface MethodSpecComponent {

    sealed interface RequiresComponent extends MethodSpecComponent {
    }

    record RequiresGuardState(GuardState guardState) implements RequiresComponent {
    }

    record RequiresPredicate(BooleanSupplier predicate) implements RequiresComponent {
    }

    sealed interface WhenComponent extends MethodSpecComponent {
        WhenCondition condition();
    }

    record WhenThenConsequence(WhenCondition condition, WhenConsequence consequence) implements WhenComponent {
        public WhenThenElseConsequence orElse(WhenConsequence elseConsequence) {
            return new WhenThenElseConsequence(condition, consequence, elseConsequence);
        }
    }

    record WhenThenElseConsequence(WhenCondition condition, WhenConsequence thenConsequence,
                                   WhenConsequence elseConsequence) implements WhenComponent {
    }

    static MethodSpecComponent requiresGuardState(final GuardState guardState) {
        return new RequiresGuardState(guardState);
    }

    static MethodSpecComponent requiresPredicate(final BooleanSupplier predicate) {
        return new RequiresPredicate(predicate);
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
