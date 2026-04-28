package com.career.planning.common;

/**
 * 业务异常类，用于 Service 层主动抛出的业务逻辑错误。
 * 区别于系统异常（NullPointerException 等），业务异常不打印完整堆栈。
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
