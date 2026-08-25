package com.transcriber.workerservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transcriber.workerservice.dto.AudioProcessingResultEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResultEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.topics.audio-processing-result}")
    private String topic;

    public void publishCompleted(UUID uploadId, List<String> questions) {
        publish(new AudioProcessingResultEvent(uploadId, "COMPLETED", questions, null));
    }

    public void publishFailed(UUID uploadId, String errorMessage) {
        publish(new AudioProcessingResultEvent(uploadId, "FAILED", null, errorMessage));
    }

    private void publish(AudioProcessingResultEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, event.getUploadId().toString(), payload);
            log.info("Published processing result: id={}, status={}", event.getUploadId(), event.getStatus());
        } catch (Exception ex) {
            log.error("CRITICAL: failed to publish processing result: id={}, error={}",
                    event.getUploadId(), ex.getMessage());
        }
    }
}
