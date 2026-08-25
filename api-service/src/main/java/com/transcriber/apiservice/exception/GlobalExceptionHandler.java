package com.transcriber.apiservice.exception;

import com.transcriber.apiservice.dto.AudioUploadErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidAudioFileException.class)
    public ResponseEntity<AudioUploadErrorResponse> handleInvalidAudioFile(InvalidAudioFileException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new AudioUploadErrorResponse(ex.getMessage(), "INVALID_AUDIO_FILE"));
    }

    @ExceptionHandler(AudioUploadException.class)
    public ResponseEntity<AudioUploadErrorResponse> handleAudioUpload(AudioUploadException ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new AudioUploadErrorResponse(ex.getMessage(), "AUDIO_UPLOAD_FAILED"));
    }

    @ExceptionHandler(AudioUploadNotFoundException.class)
    public ResponseEntity<AudioUploadErrorResponse> handleNotFound(AudioUploadNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new AudioUploadErrorResponse(ex.getMessage(), "AUDIO_UPLOAD_NOT_FOUND"));
    }
}
