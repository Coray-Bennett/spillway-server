package com.coraybennett.spillway.exception;

public class VideoConversionException extends Exception {
    public VideoConversionException(String message) {
        super(message);
    }

    public VideoConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}
