package com.wallet.smart.pay.hub.sph.outbox;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO view of WalletOutboxEvent using Java 17 record.
 * Records are immutable and lightweight; this DTO is used for in-memory processing
 * while leaving the JPA entity unchanged.
 */
public record OutboxEventDto(UUID id,
                              String aggregateType,
                              String eventType,
                              String aggregateId,
                              String payload,
                              OutboxStatus status,
                              Instant createdAt,
                              Instant publishedAt,
                              int retryCount,
                              String lastError) {
}

