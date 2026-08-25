package com.transcriber.workerservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transcriber.workerservice.dto.AudioUploadedEvent;
import com.transcriber.workerservice.service.WorkerTranscriptionOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AudioUploadListener {

    private final ObjectMapper objectMapper;
    private final WorkerTranscriptionOrchestrator orchestrator;

    @KafkaListener(topics = "${app.kafka.topics.audio-uploaded}", groupId = "${spring.kafka.consumer.group-id}")
    public void onMessage(String payload) {
        AudioUploadedEvent event;
        try {
            event = objectMapper.readValue(payload, AudioUploadedEvent.class);
        } catch (Exception ex) {
            log.error("Failed to deserialize AudioUploadedEvent, skipping message: {}", ex.getMessage());
            return;
        }
        orchestrator.handle(event);
    }
}
