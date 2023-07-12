package data.tool;

import java.nio.charset.StandardCharsets;

/**
 * @see message.SaveMessage
 */
public class ToolMessage {
    private final Status status;
    private byte[] kryo;
    private String json;

    private String errStackTrace;
    private String errMsg;

    public byte[] getKryo() {
        return this.kryo;
    }

    public String getJson() {
        return this.json;
    }

    public String getErrStackTrace() {
        return this.errStackTrace;
    }

    public String getErrMsg() {
        return this.errMsg;
    }

    public ToolMessage(String status, String kryo, String json) {
        switch (status) {
            case "ok":
                this.status = Status.OK;
                this.kryo = kryo.getBytes(StandardCharsets.ISO_8859_1);
                this.json = json;
                break;
            case "kryo":
                this.status = Status.KRYO_ERROR;
                this.errMsg = kryo.split("\n", 2)[0];
                this.errStackTrace = kryo;
                break;
            case "json":
                this.status = Status.JSON_ERROR;
                this.kryo = kryo.getBytes(StandardCharsets.ISO_8859_1);
                this.errMsg = json.split("\n", 2)[0];
                this.errStackTrace = json;
                break;
            default:
                throw new RuntimeException("Unknown inner tool message type");
        }
    }

    public Status getStatus() {
        return status;
    }
}