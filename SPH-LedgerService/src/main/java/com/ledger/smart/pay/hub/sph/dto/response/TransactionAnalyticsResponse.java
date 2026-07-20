package com.ledger.smart.pay.hub.sph.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionAnalyticsResponse(
    String userId,
    LocalDate transactionDate,
    BigDecimal totalDebit,
    BigDecimal totalCredit,
    Long transactionCount,
    BigDecimal averageAmount,
    BigDecimal netAmount
) {}

