package com.transcriber.workerservice.service;

import com.transcriber.workerservice.dto.AudioUploadedEvent;
import com.transcriber.workerservice.kafka.ResultEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkerTranscriptionOrchestrator {

    private final WorkerMinioService minioService;
    private final WorkerAudioCompressionService compressionService;
    private final GroqApiClient groqApiClient;
    private final ResultEventPublisher resultEventPublisher;

    public void handle(AudioUploadedEvent event) {
        File rawFile = null;
        File compressedFile = null;
        try {
            rawFile = minioService.downloadToTempFile(event.getBucket(), event.getObjectKey());
            log.info("Downloaded raw audio file: id={}, size={} bytes", event.getUploadId(), rawFile.length());

            compressedFile = compressionService.compress(rawFile);
            log.info("Compressed audio file: id={}, originalSize={}, compressedSize={}",
                    event.getUploadId(), rawFile.length(), compressedFile.length());

            String transcriptionText = groqApiClient.transcribe(compressedFile);
            log.info("Transcription received: id={}, length={} chars", event.getUploadId(), transcriptionText.length());

            List<String> questions = groqApiClient.extractQuestions(transcriptionText);
            log.info("Questions extracted: id={}, count={}", event.getUploadId(), questions.size());

            resultEventPublisher.publishCompleted(event.getUploadId(), questions);
            log.info("Processing completed: id={}", event.getUploadId());
        } catch (Exception ex) {
            log.error("Processing failed: id={}, error={}", event.getUploadId(), ex.getMessage());
            resultEventPublisher.publishFailed(event.getUploadId(), ex.getMessage());
        } finally {
            deleteQuietly(rawFile);
            deleteQuietly(compressedFile);
        }
    }

    private void deleteQuietly(File file) {
        if (file != null && !file.delete()) {
            log.warn("Failed to delete temp file: {}", file.getAbsolutePath());
        }
    }
}
