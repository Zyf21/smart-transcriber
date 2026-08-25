package com.transcriber.apiservice.exception;

public class AudioUploadException extends RuntimeException {

    public AudioUploadException(String message, Throwable cause) {
        super(message, cause);
    }

    public AudioUploadException(String message) {
        super(message);
    }
}
