package com.transcriber.workerservice;

import com.transcriber.workerservice.config.AudioCompressionProperties;
import com.transcriber.workerservice.config.AudioProcessingProperties;
import com.transcriber.workerservice.config.KafkaTopicProperties;
import com.transcriber.workerservice.config.MinioProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        MinioProperties.class,
        KafkaTopicProperties.class,
        AudioCompressionProperties.class,
        AudioProcessingProperties.class
})
public class WorkerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkerServiceApplication.class, args);
    }

}
