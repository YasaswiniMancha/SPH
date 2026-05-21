package com.wallet.smart.pay.hub.sph.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TransferRequest(
    @NotNull UUID fromWalletId,
    @NotNull UUID toWalletId,
    @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
    @Size(max = 255) String description
) {
}
