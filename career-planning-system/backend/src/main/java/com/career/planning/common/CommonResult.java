package com.career.planning.common;

public class CommonResult<T> {
    private long code;
    private String message;
    private T data;

    protected CommonResult() {
    }

    protected CommonResult(long code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> CommonResult<T> success(T data) {
        return new CommonResult<>(200, "操作成功", data);
    }

    public static <T> CommonResult<T> success(T data, String message) {
        return new CommonResult<>(200, message, data);
    }

    public static <T> CommonResult<T> failed(String message) {
        return new CommonResult<>(500, message, null);
    }

    public static <T> CommonResult<T> validateFailed(String message) {
        return new CommonResult<>(400, message, null);
    }

    public static <T> CommonResult<T> unauthorized(String message) {
        return new CommonResult<>(401, message, null);
    }

    public static <T> CommonResult<T> forbidden(String message) {
        return new CommonResult<>(403, message, null);
    }

    public static <T> CommonResult<T> notFound(String message) {
        return new CommonResult<>(404, message, null);
    }

    /**
     * 服务暂时不可用（外部依赖故障，如 Qdrant/Redis/AI 服务宕机）
     */
    public static <T> CommonResult<T> serviceUnavailable(String message) {
        return new CommonResult<>(503, message, null);
    }

    public long getCode() { return code; }
    public void setCode(long code) { this.code = code; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
