package com.ledger.smart.pay.hub.sph.dto.request;


import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record TransactionRequest(
    @NotBlank(message = "User ID is required")
    String userId,

    @NotBlank(message = "Wallet ID is required")
    String walletId,

    @NotBlank(message = "Merchant ID is required")
    String merchantId,

    @NotBlank(message = "Transaction type is required")
    String transactionType, // DEBIT, CREDIT, TRANSFER

    @NotNull(message = "Amount is required")
    @DecimalMin("0.01")
    BigDecimal amount,

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3)
    String currency,

    @NotBlank(message = "Description is required")
    String description,

    @NotBlank(message = "Reference ID is required")
    String referenceId
) {}