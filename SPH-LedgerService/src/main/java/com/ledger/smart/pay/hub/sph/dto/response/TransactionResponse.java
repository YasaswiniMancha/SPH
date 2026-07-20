package com.ledger.smart.pay.hub.sph.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
    String id,
    String userId,
    String walletId,
    String merchantId,
    String transactionType,
    BigDecimal amount,
    String currency,
    String status,
    String description,
    String referenceId,
    String failureReason,
    Instant createdAt,
    Instant updatedAt
) {}