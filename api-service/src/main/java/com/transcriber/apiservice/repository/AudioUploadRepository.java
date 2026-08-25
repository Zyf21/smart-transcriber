package com.transcriber.apiservice.repository;

import com.transcriber.apiservice.entity.AudioUpload;
import com.transcriber.apiservice.entity.UploadStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public interface AudioUploadRepository extends JpaRepository<AudioUpload, UUID> {

    List<AudioUpload> findAllByStatusAndCreatedAtBefore(UploadStatus status, Date threshold);
}
