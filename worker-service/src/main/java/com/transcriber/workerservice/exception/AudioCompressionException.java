package com.transcriber.workerservice.exception;

public class AudioCompressionException extends RuntimeException {

    public AudioCompressionException(String message) {
        super(message);
    }

    public AudioCompressionException(String message, Throwable cause) {
        super(message, cause);
    }
}
