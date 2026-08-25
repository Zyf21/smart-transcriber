package com.transcriber.apiservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {

    private long maxFileSizeBytes = 157_286_400L;
    private List<String> allowedContentTypes = new ArrayList<>();
    private List<String> allowedExtensions = new ArrayList<>();
    private int stalePendingThresholdMinutes = 15;
    private long cleanupIntervalMs = 300_000L;
}
