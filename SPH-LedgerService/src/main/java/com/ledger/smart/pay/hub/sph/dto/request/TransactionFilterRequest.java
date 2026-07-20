package com.ledger.smart.pay.hub.sph.dto.request;

import java.time.LocalDate;

public record TransactionFilterRequest(
    String userId,
    String walletId,
    String status,
    LocalDate startDate,
    LocalDate endDate,
    int page,
    int pageSize
) {}