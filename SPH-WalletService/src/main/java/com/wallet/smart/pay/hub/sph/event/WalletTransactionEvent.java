package com.wallet.smart.pay.hub.sph.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WalletTransactionEvent(
    UUID transactionId,
    UUID walletId,
    String type,
    BigDecimal amount,
    String currency,
    String referenceId,
    Instant occurredAt
) {
}
