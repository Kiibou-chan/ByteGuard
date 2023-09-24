package space.kiibou.byteguard.agent;

public class InstanceCapturingReqPred {

    private final String value;
    private final String prefix;

    public InstanceCapturingReqPred(String value, String prefix) {
        this.value = value;
        this.prefix = prefix;
    }

    public String unit() {
        return value;
    }

    public String concat(String other) {
        return value + other;
    }

    public String concat(String s1, String s2, String s3) {
        return value + s1 + s2 + s3;
    }

}
