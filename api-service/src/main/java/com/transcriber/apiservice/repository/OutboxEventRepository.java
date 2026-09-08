package com.transcriber.apiservice.repository;

import com.transcriber.apiservice.entity.OutboxEvent;
import com.transcriber.apiservice.entity.OutboxEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findTop50ByStatusOrderByCreatedAtAsc(OutboxEventStatus status);
}
