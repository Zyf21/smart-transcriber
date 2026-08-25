package com.transcriber.apiservice.dto;

import java.util.UUID;

public record AudioUploadedEvent(
        UUID uploadId,
        String bucket,
        String objectKey,
        String originalFilename,
        String contentType,
        Long fileSizeBytes
) {
}
