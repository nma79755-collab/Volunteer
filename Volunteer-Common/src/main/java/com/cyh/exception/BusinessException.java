package com.cyh.exception;

/**
 * Business Exception
 */
public class BusinessException extends CommonException {
    private int code = 400;

    public BusinessException(String message) {
        super(message, 400);
    }


    public BusinessException(String message, Throwable cause) {
        super(message, cause, 400);
    }

    public BusinessException(Throwable cause) {
        super(cause, 400);
    }

    public int getCode() {
        return code;
    }
}
