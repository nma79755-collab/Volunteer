package com.cyh.exception;

public class FileLoadException extends  CommonException{
    public FileLoadException(String message) {
        super(message, 500);
    }

    public FileLoadException(String message, Throwable cause) {
        super(message, cause, 500);
    }

    public FileLoadException(Throwable cause) {
        super(cause, 500);
    }

}
