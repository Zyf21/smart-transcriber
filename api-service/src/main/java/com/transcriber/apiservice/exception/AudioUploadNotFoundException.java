package com.transcriber.apiservice.exception;

public class AudioUploadNotFoundException extends RuntimeException {

    public AudioUploadNotFoundException(String message) {
        super(message);
    }
}
