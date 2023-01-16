package space.kiibou.jguard.specification.method;

import space.kiibou.jguard.specification.GuardState;

public sealed interface MethodSpecComponent {

    record RequiresGuardState(GuardState guardState) implements MethodSpecComponent {
    }

    record WhenReturnsThenConsequence(ReturnsPredicate predicate, WhenConsequence consequence) implements MethodSpecComponent {
    }

    static MethodSpecComponent requiresGuardState(final GuardState guardState) {
        return new RequiresGuardState(guardState);
    }

    static MethodSpecComponent whenReturnsThen(ReturnsPredicate predicate, WhenConsequence consequence) {
        return new WhenReturnsThenConsequence(predicate, consequence);
    }

}
