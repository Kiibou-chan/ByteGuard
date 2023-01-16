package space.kiibou.jguard.exception;

public class WrongReturnValueException extends RuntimeException {

    public WrongReturnValueException(String className, String methodName, boolean returnValue, boolean expectedValue) {
        super("Method " + className + "#" + methodName + " returned " + returnValue + " but " + expectedValue + " was expected");
    }

    public WrongReturnValueException(String className, String methodName, int returnValue, int expectedValue) {
        super("Method " + className + "#" + methodName + " returned " + returnValue + " but " + expectedValue + " was expected");
    }

    public WrongReturnValueException(String className, String methodName, long returnValue, long expectedValue) {
        super("Method " + className + "#" + methodName + " returned " + returnValue + " but " + expectedValue + " was expected");
    }

    public WrongReturnValueException(String className, String methodName, float returnValue, float expectedValue) {
        super("Method " + className + "#" + methodName + " returned " + returnValue + " but " + expectedValue + " was expected");
    }

    public WrongReturnValueException(String className, String methodName, double returnValue, double expectedValue) {
        super("Method " + className + "#" + methodName + " returned " + returnValue + " but " + expectedValue + " was expected");
    }

    public WrongReturnValueException(String className, String methodName, Object returnValue, Object expectedValue) {
        super("Method " + className + "#" + methodName + " returned " + returnValue + " but " + expectedValue + " was expected");
    }

}
