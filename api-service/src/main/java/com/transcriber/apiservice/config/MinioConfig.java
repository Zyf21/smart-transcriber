package com.transcriber.apiservice.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties({MinioProperties.class, UploadProperties.class})
public class MinioConfig {

    @Bean
    MinioClient minioClient(MinioProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }

    @Bean
    ApplicationRunner minioBucketInitializer(MinioClient minioClient, MinioProperties properties) {
        return args -> {
            String bucket = properties.getBucketRaw();
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build()
            );
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Created MinIO bucket: {}", bucket);
            } else {
                log.info("MinIO bucket already exists: {}", bucket);
            }
        };
    }
}
