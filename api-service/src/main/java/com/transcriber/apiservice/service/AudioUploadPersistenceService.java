package com.transcriber.apiservice.service;



import com.fasterxml.jackson.core.JsonProcessingException;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.transcriber.apiservice.dto.AudioUploadedEvent;

import com.transcriber.apiservice.entity.AudioUpload;

import com.transcriber.apiservice.entity.OutboxEvent;

import com.transcriber.apiservice.entity.OutboxEventStatus;

import com.transcriber.apiservice.entity.OutboxEventType;

import com.transcriber.apiservice.entity.UploadStatus;

import com.transcriber.apiservice.repository.AudioUploadRepository;

import com.transcriber.apiservice.repository.OutboxEventRepository;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Propagation;

import org.springframework.transaction.annotation.Transactional;



import java.util.Date;
import java.util.List;
import java.util.UUID;



@Slf4j

@Service

@RequiredArgsConstructor

public class AudioUploadPersistenceService {



    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;



    private final AudioUploadRepository audioUploadRepository;

    private final OutboxEventRepository outboxEventRepository;

    private final ObjectMapper objectMapper;



    @Transactional(propagation = Propagation.REQUIRES_NEW)

    public AudioUpload createPendingUpload(

            UUID id,

            String originalFilename,

            String contentType,

            long fileSizeBytes,

            String bucket,

            String objectKey

    ) {

        AudioUpload upload = AudioUpload.builder()

                .id(id)

                .originalFilename(originalFilename)

                .contentType(contentType)

                .fileSizeBytes(fileSizeBytes)

                .bucket(bucket)

                .objectKey(objectKey)

                .status(UploadStatus.PENDING)

                .build();

        return audioUploadRepository.save(upload);

    }



    @Transactional

    public AudioUpload markUploaded(UUID id) {

        AudioUpload upload = getRequiredUpload(id);

        upload.setStatus(UploadStatus.UPLOADED);

        upload.setErrorMessage(null);

        audioUploadRepository.save(upload);



        AudioUploadedEvent event = new AudioUploadedEvent(

                upload.getId(),

                upload.getBucket(),

                upload.getObjectKey(),

                upload.getOriginalFilename(),

                upload.getContentType(),

                upload.getFileSizeBytes()

        );



        OutboxEvent outboxEvent = OutboxEvent.builder()

                .id(UUID.randomUUID())

                .aggregateId(id)

                .eventType(OutboxEventType.AUDIO_UPLOADED)

                .payload(serializeEvent(event))

                .status(OutboxEventStatus.NEW)

                .build();

        outboxEventRepository.save(outboxEvent);



        return upload;

    }



    @Transactional

    public void markCompleted(UUID id, List<String> questions) {

        AudioUpload upload = getRequiredUpload(id);

        if (upload.getStatus() == UploadStatus.COMPLETED || upload.getStatus() == UploadStatus.FAILED) {

            log.warn("Ignoring duplicate result for already finalized upload: id={}, currentStatus={}",

                    id, upload.getStatus());

            return;

        }

        upload.setStatus(UploadStatus.COMPLETED);

        upload.setQuestions(serializeQuestions(questions));

        upload.setCompletedAt(new Date());

        audioUploadRepository.save(upload);

    }



    @Transactional

    public AudioUpload markFailed(UUID id, String errorMessage) {

        AudioUpload upload = getRequiredUpload(id);

        if (upload.getStatus() == UploadStatus.COMPLETED || upload.getStatus() == UploadStatus.FAILED) {

            log.warn("Ignoring duplicate result for already finalized upload: id={}, currentStatus={}",

                    id, upload.getStatus());

            return upload;

        }

        upload.setStatus(UploadStatus.FAILED);

        upload.setErrorMessage(truncateErrorMessage(errorMessage));

        return audioUploadRepository.save(upload);

    }



    private AudioUpload getRequiredUpload(UUID id) {

        return audioUploadRepository.findById(id)

                .orElseThrow(() -> new IllegalStateException("Audio upload not found: " + id));

    }



    private String serializeEvent(AudioUploadedEvent event) {

        try {

            return objectMapper.writeValueAsString(event);

        } catch (JsonProcessingException ex) {

            throw new IllegalStateException("Failed to serialize audio uploaded event", ex);

        }

    }



    private String serializeQuestions(List<String> questions) {

        try {

            return objectMapper.writeValueAsString(questions != null ? questions : List.of());

        } catch (Exception ex) {

            log.error("Failed to serialize questions: {}", ex.getMessage());

            return "[]";

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


