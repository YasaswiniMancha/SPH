package com.wallet.smart.pay.hub.sph.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TransactionResponse(
    UUID transactionId,
    UUID walletId,
    String type,
    BigDecimal amount,
    String currency,
    String referenceId,
    String description
) {
}
