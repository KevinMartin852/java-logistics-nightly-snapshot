package dev.infrai.logistics.storage;

import java.util.Map;

public final class InfraiException extends RuntimeException {
    private final int statusCode;
    private final String code;

    private InfraiException(int statusCode, String code, String message) {
        super(message);
        this.statusCode = statusCode;
        this.code = code;
    }

    static InfraiException from(int statusCode, Object errorValue) {
        if (errorValue instanceof Map<?, ?> error) {
            Object codeValue = error.containsKey("code") ? error.get("code") : "REQUEST_REJECTED";
            String code = String.valueOf(codeValue);
            Object detail = error.containsKey("hint") ? error.get("hint") : error.get("message");
            return new InfraiException(statusCode, code, String.valueOf(detail));
        }
        return new InfraiException(statusCode, "REQUEST_REJECTED", "Request was rejected");
    }

    public int statusCode() { return statusCode; }
    public String code() { return code; }
}
