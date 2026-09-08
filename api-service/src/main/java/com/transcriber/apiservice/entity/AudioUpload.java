package com.transcriber.apiservice.entity;



import jakarta.persistence.Column;

import jakarta.persistence.Entity;

import jakarta.persistence.EnumType;

import jakarta.persistence.Enumerated;

import jakarta.persistence.Id;

import jakarta.persistence.PrePersist;

import jakarta.persistence.PreUpdate;

import jakarta.persistence.Table;

import jakarta.persistence.Temporal;

import jakarta.persistence.TemporalType;

import lombok.AllArgsConstructor;

import lombok.Builder;

import lombok.Getter;

import lombok.NoArgsConstructor;

import lombok.Setter;



import java.util.Date;

import java.util.UUID;



@Entity

@Table(name = "audio_uploads")

@Getter

@Setter

@Builder

@NoArgsConstructor

@AllArgsConstructor

public class AudioUpload {



    @Id

    private UUID id;



    @Column(name = "original_filename", nullable = false)

    private String originalFilename;



    @Column(name = "content_type")

    private String contentType;



    @Column(name = "file_size_bytes")

    private Long fileSizeBytes;



    @Column(nullable = false)

    private String bucket;



    @Column(name = "object_key", nullable = false, unique = true)

    private String objectKey;



    @Enumerated(EnumType.STRING)

    @Column(nullable = false)

    private UploadStatus status;



    @Column(name = "error_message", length = 500)

    private String errorMessage;



    @Column(columnDefinition = "TEXT")

    private String questions;



    @Temporal(TemporalType.TIMESTAMP)

    @Column(name = "completed_at")

    private Date completedAt;



    @Temporal(TemporalType.TIMESTAMP)

    @Column(name = "created_at", nullable = false)

    private Date createdAt;



    @Temporal(TemporalType.TIMESTAMP)

    @Column(name = "updated_at", nullable = false)

    private Date updatedAt;



    @PrePersist

    void onCreate() {

        Date now = new Date();

        if (createdAt == null) {

            createdAt = now;

        }

        updatedAt = now;

    }



    @PreUpdate

    void onUpdate() {

        updatedAt = new Date();

    }

}


