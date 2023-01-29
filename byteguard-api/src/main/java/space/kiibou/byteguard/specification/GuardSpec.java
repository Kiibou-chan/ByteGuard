package space.kiibou.byteguard.specification;

import space.kiibou.byteguard.Guard;
import space.kiibou.byteguard.specification.method.*;

import java.util.function.BooleanSupplier;

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

    protected final WhenCondition when(final ReturnsPredicate predicate) {
        return new WhenCondition.Returns(predicate);
    }

    protected final WhenCondition when(final GuardState state) {
        return new WhenCondition.GuardCondition(state);
    }

    protected final ReturnsPredicate returns() {
        return ReturnsPredicate.noArgs();
    }

    protected final ReturnsPredicate returns(Object value) {
        return ReturnsPredicate.value(value);
    }

    protected final WhenConsequence set(final Guard guard) {
        return WhenConsequence.setGuard(guard);
    }

    protected final WhenConsequence reset(final Guard guard) {
        return WhenConsequence.resetGuard(guard);
    }

    protected final MethodSpecComponent requires(final GuardState state) {
        return MethodSpecComponent.requiresGuardState(state);
    }

    protected final MethodSpecComponent requires(final BooleanSupplier predicate) {
        return MethodSpecComponent.requiresPredicate(predicate);
    }

}
