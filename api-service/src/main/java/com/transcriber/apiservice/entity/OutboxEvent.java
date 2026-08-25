package com.transcriber.apiservice.entity;



import jakarta.persistence.Column;

import jakarta.persistence.Entity;

import jakarta.persistence.EnumType;

import jakarta.persistence.Enumerated;

import jakarta.persistence.Id;

import jakarta.persistence.PrePersist;

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

@Table(name = "outbox_events")

@Getter

@Setter

@Builder

@NoArgsConstructor

@AllArgsConstructor

public class OutboxEvent {



    @Id

    private UUID id;



    @Column(name = "aggregate_id", nullable = false)

    private UUID aggregateId;



    @Column(name = "event_type", nullable = false, length = 64)

    private String eventType;



    @Column(nullable = false, columnDefinition = "TEXT")

    private String payload;



    @Enumerated(EnumType.STRING)

    @Column(nullable = false)

    private OutboxEventStatus status;



    @Column(name = "error_message", length = 500)

    private String errorMessage;



    @Temporal(TemporalType.TIMESTAMP)

    @Column(name = "created_at", nullable = false)

    private Date createdAt;



    @Temporal(TemporalType.TIMESTAMP)

    @Column(name = "sent_at")

    private Date sentAt;



    @PrePersist

    void onCreate() {

        if (createdAt == null) {

            createdAt = new Date();

        }

    }

}


