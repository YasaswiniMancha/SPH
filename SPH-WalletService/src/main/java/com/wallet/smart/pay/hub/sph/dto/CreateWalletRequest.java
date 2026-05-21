package com.wallet.smart.pay.hub.sph.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateWalletRequest(
    @NotBlank @Size(max = 100) String customerId,
    @NotBlank @Size(min = 3, max = 3) String currency,
    @NotNull @DecimalMin(value = "0.00") BigDecimal openingBalance
) {
}
