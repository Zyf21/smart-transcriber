package com.transcriber.apiservice.dto;

import com.transcriber.apiservice.entity.UploadStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AudioUploadResponse {

    private UUID id;
    private UploadStatus status;
    private String originalFilename;
    private Long fileSizeBytes;
    private Date createdAt;
    private List<String> questions;
    private String errorMessage;
}
