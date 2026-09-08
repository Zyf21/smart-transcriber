package com.transcriber.apiservice.service;

import com.transcriber.apiservice.config.KafkaTopicProperties;
import com.transcriber.apiservice.entity.OutboxEvent;
import com.transcriber.apiservice.entity.OutboxEventStatus;
import com.transcriber.apiservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisherScheduler {

    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaEventPublisher kafkaEventPublisher;
    private final KafkaTopicProperties kafkaTopicProperties;

    @Scheduled(fixedDelayString = "${app.outbox.publish-interval-ms:5000}")
    public void publishPendingEvents() {
        List<OutboxEvent> events = outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxEventStatus.NEW);
        String topicName = kafkaTopicProperties.getTopics().getAudioUploaded();

        for (OutboxEvent event : events) {
            try {
                kafkaEventPublisher.publish(topicName, event.getAggregateId().toString(), event.getPayload());
                event.setStatus(OutboxEventStatus.SENT);
                event.setSentAt(new Date());
                event.setErrorMessage(null);
            } catch (Exception ex) {
                log.error("Failed to publish outbox event: id={}, error={}", event.getId(), ex.getMessage());
                event.setStatus(OutboxEventStatus.FAILED);
                event.setErrorMessage(truncateErrorMessage(ex.getMessage()));
            }
            outboxEventRepository.save(event);
        }
    }

    private String truncateErrorMessage(String errorMessage) {
        if (errorMessage == null) {
            return null;
        }
        if (errorMessage.length() <= MAX_ERROR_MESSAGE_LENGTH) {
            return errorMessage;
        }
        return errorMessage.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }
}
