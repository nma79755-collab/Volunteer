package com.cyh.exception;

public class FeignException extends CommonException{
    private int code = 400;

    public FeignException(String message) {
        super(message, 400);
    }


    public FeignException(String message, Throwable cause) {
        super(message, cause, 400);
    }

    public FeignException(Throwable cause) {
        super(cause, 400);
    }

    public int getCode() {
        return code;
    }
}

