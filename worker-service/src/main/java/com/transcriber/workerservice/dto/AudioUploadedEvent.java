package com.transcriber.workerservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AudioUploadedEvent {

    private UUID uploadId;
    private String bucket;
    private String objectKey;
    private String originalFilename;
    private String contentType;
    private Long fileSizeBytes;
}
