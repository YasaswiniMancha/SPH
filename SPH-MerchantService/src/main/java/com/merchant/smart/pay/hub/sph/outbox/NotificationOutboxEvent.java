package com.merchant.smart.pay.hub.sph.outbox;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_outbox_events", indexes = {
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationOutboxEvent {

    @Id
    @Column(columnDefinition = "UUID")
    private String id = UUID.randomUUID().toString();

    @Column(nullable = false)
    private String aggregateType;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String aggregateId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxEventStatus status = OutboxEventStatus.PENDING;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column
    private Instant publishedAt;

    @Column
    private int retryCount = 0;

    @Column
    private int maxRetries = 3;

    @Column(length = 500)
    private String lastError;

    @Version
    private Long version;

    public boolean canRetry() {
        return retryCount < maxRetries && status == OutboxEventStatus.FAILED;
    }

    public void markPublished() {
        this.status = OutboxEventStatus.PUBLISHED;
        this.publishedAt = Instant.now();
    }

    public void markFailed(String error) {
        this.status = OutboxEventStatus.FAILED;
        this.lastError = error;
    }
}