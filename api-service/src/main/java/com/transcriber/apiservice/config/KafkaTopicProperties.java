package com.transcriber.apiservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.kafka")
public class KafkaTopicProperties {

    private Topics topics = new Topics();

    @Getter
    @Setter
    public static class Topics {

        private String audioUploaded = "audio-uploaded";
        private String audioProcessingResult = "audio-processing-results";
    }
}
