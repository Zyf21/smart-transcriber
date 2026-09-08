package com.transcriber.apiservice.dto;

public record AudioUploadErrorResponse(
        String message,
        String errorCode
) {
}
