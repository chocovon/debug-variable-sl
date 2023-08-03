package util.code;

public class Code {
    private final String code;
    private final ObjectCode objectCode;

    public Code(String code) {
        this.code = code;
        this.objectCode = null;
    }

    public Code(String code, ObjectCode objectCode) {
        this.code = code;
        this.objectCode = objectCode;
        this.objectCode.referenceCount++;
    }

    public String getCode() {
        return code;
    }
}
