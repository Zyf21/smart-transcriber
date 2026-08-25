package com.transcriber.apiservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transcriber.apiservice.dto.AudioProcessingResultEvent;
import com.transcriber.apiservice.service.AudioUploadPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AudioProcessingResultListener {

    private final ObjectMapper objectMapper;
    private final AudioUploadPersistenceService persistenceService;

    @KafkaListener(topics = "${app.kafka.topics.audio-processing-result}", groupId = "${spring.kafka.consumer.group-id}")
    public void onMessage(String payload) {
        AudioProcessingResultEvent event;
        try {
            event = objectMapper.readValue(payload, AudioProcessingResultEvent.class);
        } catch (Exception ex) {
            log.error("Failed to deserialize AudioProcessingResultEvent, skipping: {}", ex.getMessage());
            return;
        }

        if ("COMPLETED".equals(event.getStatus())) {
            persistenceService.markCompleted(event.getUploadId(), event.getQuestions());
        } else if ("FAILED".equals(event.getStatus())) {
            persistenceService.markFailed(event.getUploadId(), event.getErrorMessage());
        } else {
            log.warn("Unknown status in AudioProcessingResultEvent: id={}, status={}", event.getUploadId(), event.getStatus());
        }
    }
}
