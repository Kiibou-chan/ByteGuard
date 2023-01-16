package space.kiibou.jguard.specification;

import space.kiibou.jguard.Guard;
import space.kiibou.jguard.specification.method.MethodSpec;
import space.kiibou.jguard.specification.method.MethodSpecComponent;
import space.kiibou.jguard.specification.method.ReturnsPredicate;
import space.kiibou.jguard.specification.method.WhenConsequence;

public abstract class GuardSpec {

    private int guardIndex = 0;

    protected final Guard guard() {
        return new Guard(this.getClass(), guardIndex++, Guard.State.RESET);
    }

    protected final Guard guard(final Guard.State initialState) {
        return new Guard(this.getClass(), guardIndex++, initialState);
    }

    protected final MethodSpec methodSpec(final MethodSpecComponent... args) {
        return new MethodSpec(args);
    }

    protected final MethodSpecComponent when(final ReturnsPredicate predicate, final WhenConsequence consequence) {
        return MethodSpecComponent.whenReturnsThen(predicate, consequence);
    }

    protected final ReturnsPredicate returns() {
        return ReturnsPredicate.noArgs();
    }

    protected final ReturnsPredicate returns(Object value) {
        return ReturnsPredicate.value(value);
    }

    protected final WhenConsequence sets(final Guard guard) {
        return WhenConsequence.setsGuard(guard);
    }

    protected final WhenConsequence resets(final Guard guard) {
        return WhenConsequence.resetsGuard(guard);
    }

    protected final MethodSpecComponent requires(final GuardState state) {
        return MethodSpecComponent.requiresGuardState(state);
    }

}
