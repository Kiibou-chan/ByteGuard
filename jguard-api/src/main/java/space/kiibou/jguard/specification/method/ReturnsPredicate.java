package space.kiibou.jguard.specification.method;

public sealed interface ReturnsPredicate {

    record NoArgs() implements ReturnsPredicate {
    }

    record Value(Object value) implements ReturnsPredicate {
    }

    static ReturnsPredicate noArgs() {
        return new NoArgs();
    }

    static ReturnsPredicate value(Object value) {
        return new Value(value);
    }

}
