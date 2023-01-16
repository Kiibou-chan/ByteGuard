package space.kiibou.jguard;

import space.kiibou.jguard.specification.GuardSpec;
import space.kiibou.jguard.specification.GuardState;

public record Guard(Class<? extends GuardSpec> belongsTo, int index, State initialState) {

    public GuardState isSet() {
        return new GuardState(this, State.SET);
    }

    public GuardState isReset() {
        return new GuardState(this, State.RESET);
    }

    public enum State {
        SET(true),
        RESET(false);

        final boolean booleanValue;

        State(boolean booleanValue) {
            this.booleanValue = booleanValue;
        }

        public State negate() {
            if (this == SET) return RESET;
            else return SET;
        }
    }

}
