package com.transcriber.workerservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.audio-compression")
public class AudioCompressionProperties {

    private long maxOutputSizeBytes = 25_000_000L;
    private int targetSampleRateHz = 16_000;
    private int minBitrateKbps = 16;
    private int maxBitrateKbps = 64;
    private double bitrateSafetyFactor = 0.9;
}
