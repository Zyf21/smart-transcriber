package com.transcriber.workerservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.audio-processing")
public class AudioProcessingProperties {

    private int maxQuestionExtractionTokens = 8_000;
}
