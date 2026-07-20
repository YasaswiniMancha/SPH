package com.ledger.smart.pay.hub.sph.kafka.event;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionEvent(
    String transactionId,
    String userId,
    String status,
    BigDecimal amount,
    Instant timestamp
) {}