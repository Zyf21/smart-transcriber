package com.transcriber.workerservice.service;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkerMinioService {

    private final MinioClient minioClient;

    public File downloadToTempFile(String bucket, String objectKey) {
        try (InputStream inputStream = minioClient.getObject(
                GetObjectArgs.builder().bucket(bucket).object(objectKey).build())) {
            File tempFile = Files.createTempFile("audio-raw-", "-" + extractExtension(objectKey)).toFile();
            Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return tempFile;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to download object from MinIO: bucket=" + bucket + ", key=" + objectKey, ex);
        }
    }

    private String extractExtension(String objectKey) {
        int dotIndex = objectKey.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == objectKey.length() - 1) {
            return "bin";
        }
        return objectKey.substring(dotIndex + 1);
    }
}
