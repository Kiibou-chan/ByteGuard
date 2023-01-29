package space.kiibou.byteguard.exception;

public class GuardViolationException extends RuntimeException {

    public GuardViolationException(String message) {
        super(message);
    }

}
