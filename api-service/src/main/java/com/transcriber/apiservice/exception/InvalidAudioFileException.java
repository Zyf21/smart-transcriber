package com.transcriber.apiservice.exception;

public class InvalidAudioFileException extends RuntimeException {

    public InvalidAudioFileException(String message) {
        super(message);
    }
}
