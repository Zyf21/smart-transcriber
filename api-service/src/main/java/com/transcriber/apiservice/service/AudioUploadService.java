package com.transcriber.apiservice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transcriber.apiservice.config.MinioProperties;
import com.transcriber.apiservice.config.UploadProperties;
import com.transcriber.apiservice.dto.AudioUploadResponse;
import com.transcriber.apiservice.entity.AudioUpload;
import com.transcriber.apiservice.entity.UploadStatus;
import com.transcriber.apiservice.exception.AudioUploadException;
import com.transcriber.apiservice.exception.AudioUploadNotFoundException;
import com.transcriber.apiservice.exception.InvalidAudioFileException;
import com.transcriber.apiservice.repository.AudioUploadRepository;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudioUploadService {

    private static final String OBJECT_KEY_PREFIX = "audio/";
    private static final String STALE_PENDING_ERROR_MESSAGE = "Upload timeout, stale PENDING record";

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final UploadProperties uploadProperties;
    private final AudioUploadRepository audioUploadRepository;
    private final AudioUploadPersistenceService persistenceService;
    private final ObjectMapper objectMapper;

    public AudioUploadResponse uploadAudio(MultipartFile file) {
        validateFile(file);

        UUID id = UUID.randomUUID();
        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown"
        );
        String objectKey = buildObjectKey(id, originalFilename);
        String bucket = minioProperties.getBucketRaw();

        log.info("Starting audio upload: id={}, filename={}, size={}", id, originalFilename, file.getSize());

        persistenceService.createPendingUpload(
                id,
                originalFilename,
                file.getContentType(),
                file.getSize(),
                bucket,
                objectKey
        );

        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
        } catch (Exception ex) {
            log.error("MinIO upload failed: id={}, filename={}, error={}", id, originalFilename, ex.getMessage());
            persistenceService.markFailed(id, ex.getMessage());
            throw new AudioUploadException("Failed to upload audio file to storage", ex);
        }

        AudioUpload uploaded = persistenceService.markUploaded(id);

        log.info("Audio upload completed: id={}, filename={}, size={}", id, originalFilename, file.getSize());
        return toResponse(uploaded);
    }

    @Cacheable(value = "audioUploads", key = "#id", unless = "#result.getStatus().name() != 'COMPLETED'")
    public AudioUploadResponse getUpload(UUID id) {
        AudioUpload upload = audioUploadRepository.findById(id)
                .orElseThrow(() -> new AudioUploadNotFoundException("Audio upload not found: " + id));
        return toResponse(upload);
    }

    @Scheduled(fixedDelayString = "${app.upload.cleanup-interval-ms:300000}")
    public void cleanupStalePendingUploads() {
        Date threshold = new Date(System.currentTimeMillis()
                - Duration.ofMinutes(uploadProperties.getStalePendingThresholdMinutes()).toMillis());
        var staleUploads = audioUploadRepository.findAllByStatusAndCreatedAtBefore(UploadStatus.PENDING, threshold);

        if (staleUploads.isEmpty()) {
            log.debug("No stale PENDING audio uploads found");
            return;
        }

        log.debug("Found {} stale PENDING audio uploads older than {} minutes",
                staleUploads.size(), uploadProperties.getStalePendingThresholdMinutes());

        for (AudioUpload upload : staleUploads) {
            log.warn("Marking stale PENDING upload as FAILED: id={}, createdAt={}",
                    upload.getId(), upload.getCreatedAt());
            persistenceService.markFailed(upload.getId(), STALE_PENDING_ERROR_MESSAGE);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidAudioFileException("Audio file is empty");
        }

        if (file.getSize() > uploadProperties.getMaxFileSizeBytes()) {
            throw new InvalidAudioFileException(
                    "File size exceeds maximum allowed size of " + uploadProperties.getMaxFileSizeBytes() + " bytes"
            );
        }

        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.toLowerCase(Locale.ROOT).startsWith("audio/")) {
            throw new InvalidAudioFileException("Content type must start with 'audio/'");
        }

        String normalizedContentType = contentType.toLowerCase(Locale.ROOT);
        boolean contentTypeAllowed = uploadProperties.getAllowedContentTypes().stream()
                .map(type -> type.toLowerCase(Locale.ROOT))
                .anyMatch(normalizedContentType::equals);

        String extension = extractExtension(file.getOriginalFilename());
        boolean extensionAllowed = extension != null && uploadProperties.getAllowedExtensions().stream()
                .map(ext -> ext.toLowerCase(Locale.ROOT))
                .anyMatch(extension::equals);

        if (!contentTypeAllowed && !extensionAllowed) {
            throw new InvalidAudioFileException("File type is not allowed");
        }
    }

    private String buildObjectKey(UUID id, String originalFilename) {
        return OBJECT_KEY_PREFIX + id + "/" + originalFilename;
    }

    private String extractExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return null;
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return null;
        }
        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private AudioUploadResponse toResponse(AudioUpload upload) {
        return new AudioUploadResponse(
                upload.getId(),
                upload.getStatus(),
                upload.getOriginalFilename(),
                upload.getFileSizeBytes(),
                upload.getCreatedAt(),
                deserializeQuestions(upload.getQuestions()),
                upload.getErrorMessage()
        );
    }

    private List<String> deserializeQuestions(String questionsJson) {
        if (!StringUtils.hasText(questionsJson)) {
            return null;
        }
        try {
            return objectMapper.readValue(questionsJson, new TypeReference<List<String>>() {});
        } catch (Exception ex) {
            log.error("Failed to deserialize questions: {}", ex.getMessage());
            return null;
        }
    }
}
