package com.merchant.smart.pay.hub.sph.outbox;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "merchant_outbox_events", indexes = {
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantOutboxEvent {

    @Id
    @Column(columnDefinition = "UUID")
    private String id = UUID.randomUUID().toString();

    @Column(nullable = false, length = 150)
    private String aggregateType; // MERCHANT

    @Column(nullable = false, length = 80)
    private String eventType; // MERCHANT_CREATED, MERCHANT_APPROVED, etc.

    @Column(nullable = false, length = 80)
    private String aggregateId; // merchant ID

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload; // JSON serialized event

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxEventStatus status; // PENDING, PUBLISHED, FAILED

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

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (status == null) {
            status = OutboxEventStatus.PENDING;
        }
    }

    public boolean canRetry() {
        return retryCount < maxRetries && status == OutboxEventStatus.FAILED;
    }

    public void incrementRetry() {
        this.retryCount++;
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