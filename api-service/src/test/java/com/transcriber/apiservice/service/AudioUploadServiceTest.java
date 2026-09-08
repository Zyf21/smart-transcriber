package com.transcriber.apiservice.service;

import com.transcriber.apiservice.config.MinioProperties;
import com.transcriber.apiservice.config.UploadProperties;
import com.transcriber.apiservice.dto.AudioUploadResponse;
import com.transcriber.apiservice.entity.AudioUpload;
import com.transcriber.apiservice.entity.UploadStatus;
import com.transcriber.apiservice.exception.AudioUploadException;
import com.transcriber.apiservice.exception.InvalidAudioFileException;
import com.transcriber.apiservice.repository.AudioUploadRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AudioUploadServiceTest {

    private static final String BUCKET = "raw-audio";
    private static final byte[] AUDIO_CONTENT = "audio-content".getBytes();

    @Mock
    private MinioClient minioClient;

    @Mock
    private MinioProperties minioProperties;

    @Mock
    private UploadProperties uploadProperties;

    @Mock
    private AudioUploadRepository audioUploadRepository;

    @Mock
    private AudioUploadPersistenceService persistenceService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AudioUploadService audioUploadService;

    @BeforeEach
    void setUp() {
        when(minioProperties.getBucketRaw()).thenReturn(BUCKET);
        when(uploadProperties.getMaxFileSizeBytes()).thenReturn(157_286_400L);
        when(uploadProperties.getAllowedContentTypes()).thenReturn(List.of("audio/mpeg", "audio/wav"));
        when(uploadProperties.getAllowedExtensions()).thenReturn(List.of("mp3", "wav"));
        when(uploadProperties.getStalePendingThresholdMinutes()).thenReturn(15);
    }

    @Test
    void uploadAudio_success_marksUploadAsUploaded() throws Exception {
        MultipartFile file = createValidAudioFile("track.mp3", "audio/mpeg");
        Date createdAt = new Date();

        when(persistenceService.createPendingUpload(
                any(UUID.class),
                anyString(),
                anyString(),
                anyLong(),
                anyString(),
                anyString()
        )).thenAnswer(invocation -> createUpload(
                invocation.getArgument(0),
                UploadStatus.PENDING,
                createdAt
        ));
        when(persistenceService.markUploaded(any(UUID.class))).thenAnswer(invocation -> createUpload(
                invocation.getArgument(0),
                UploadStatus.UPLOADED,
                createdAt
        ));

        AudioUploadResponse response = audioUploadService.uploadAudio(file);

        assertThat(response.getStatus()).isEqualTo(UploadStatus.UPLOADED);
        assertThat(response.getOriginalFilename()).isEqualTo("track.mp3");
        assertThat(response.getFileSizeBytes()).isEqualTo((long) AUDIO_CONTENT.length);

        verify(minioClient).putObject(any(PutObjectArgs.class));
        verify(persistenceService).markUploaded(any(UUID.class));
        verify(persistenceService, never()).markFailed(any(UUID.class), anyString());
    }

    @Test
    void uploadAudio_minioFailure_marksUploadAsFailed() throws Exception {
        MultipartFile file = createValidAudioFile("track.mp3", "audio/mpeg");

        when(persistenceService.createPendingUpload(
                any(UUID.class),
                anyString(),
                anyString(),
                anyLong(),
                anyString(),
                anyString()
        )).thenAnswer(invocation -> createUpload(
                invocation.getArgument(0),
                UploadStatus.PENDING,
                new Date()
        ));
        doThrow(new RuntimeException("MinIO unavailable")).when(minioClient).putObject(any(PutObjectArgs.class));

        assertThatThrownBy(() -> audioUploadService.uploadAudio(file))
                .isInstanceOf(AudioUploadException.class)
                .hasMessageContaining("Failed to upload audio file to storage");

        verify(persistenceService).markFailed(any(UUID.class), anyString());
        verify(persistenceService, never()).markUploaded(any(UUID.class));
    }

    @Test
    void uploadAudio_invalidContentType_doesNotCreateDbRecord() {
        MultipartFile file = new MockMultipartFile(
                "file",
                "track.txt",
                "text/plain",
                AUDIO_CONTENT
        );

        assertThatThrownBy(() -> audioUploadService.uploadAudio(file))
                .isInstanceOf(InvalidAudioFileException.class)
                .hasMessageContaining("Content type must start with 'audio/'");

        verifyNoInteractions(persistenceService);
    }

    @Test
    void uploadAudio_fileTooLarge_doesNotCreateDbRecord() {
        when(uploadProperties.getMaxFileSizeBytes()).thenReturn(10L);
        MultipartFile file = createValidAudioFile("track.mp3", "audio/mpeg");

        assertThatThrownBy(() -> audioUploadService.uploadAudio(file))
                .isInstanceOf(InvalidAudioFileException.class)
                .hasMessageContaining("File size exceeds maximum allowed size");

        verifyNoInteractions(persistenceService);
    }

    @Test
    void uploadAudio_emptyFile_doesNotCreateDbRecord() {
        MultipartFile file = new MockMultipartFile("file", "track.mp3", "audio/mpeg", new byte[0]);

        assertThatThrownBy(() -> audioUploadService.uploadAudio(file))
                .isInstanceOf(InvalidAudioFileException.class)
                .hasMessageContaining("Audio file is empty");

        verifyNoInteractions(persistenceService);
    }

    @Test
    void cleanupStalePendingUploads_marksOldPendingRecordsAsFailed() {
        UUID staleId = UUID.randomUUID();
        Date staleCreatedAt = new Date(System.currentTimeMillis() - Duration.ofMinutes(20).toMillis());
        AudioUpload staleUpload = createUpload(staleId, UploadStatus.PENDING, staleCreatedAt);

        when(audioUploadRepository.findAllByStatusAndCreatedAtBefore(any(), any()))
                .thenReturn(List.of(staleUpload));

        audioUploadService.cleanupStalePendingUploads();

        ArgumentCaptor<String> errorCaptor = ArgumentCaptor.forClass(String.class);
        verify(persistenceService).markFailed(eq(staleId), errorCaptor.capture());
        assertThat(errorCaptor.getValue()).isEqualTo("Upload timeout, stale PENDING record");
    }

    private MultipartFile createValidAudioFile(String filename, String contentType) {
        return new MockMultipartFile("file", filename, contentType, AUDIO_CONTENT);
    }

    private AudioUpload createUpload(UUID id, UploadStatus status, Date createdAt) {
        return AudioUpload.builder()
                .id(id)
                .originalFilename("track.mp3")
                .contentType("audio/mpeg")
                .fileSizeBytes((long) AUDIO_CONTENT.length)
                .bucket(BUCKET)
                .objectKey("audio/" + id + "/track.mp3")
                .status(status)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
    }
}
