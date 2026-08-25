package com.transcriber.workerservice.exception;

public class GroqApiException extends RuntimeException {

    private final int statusCode;

    public GroqApiException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public GroqApiException(int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
