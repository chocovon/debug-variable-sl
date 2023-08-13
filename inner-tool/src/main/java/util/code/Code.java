package util.code;

public class Code {
    private final String code; // value or reference
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
        if (objectCode != null && objectCode.forceInline) {
            return objectCode.generateInlineCode();
        }
        return code;
    }

    public boolean isNull() {
        return code.equals("null");
    }

}
