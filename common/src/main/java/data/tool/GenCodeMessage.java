package data.tool;

/**
 * @see message.GenCodeMessage
 */
public class GenCodeMessage {
    private final Status status;
    private String code;
    private String err;

    public GenCodeMessage(String status, String code, String err) {
        if ("ok".equals(status)) {
            this.status = Status.OK;
            this.code = code;
        } else {
            this.status = Status.KRYO_ERROR;
            this.err = err;
        }
    }

    public String getCode() {
        return this.code;
    }

    public String getErr() {
        return err;
    }

    public Status getStatus() {
        return status;
    }
}