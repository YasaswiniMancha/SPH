package com.wallet.smart.pay.hub.sph.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AmountRequest(
    @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
    @NotBlank @Size(min = 3, max = 3) String currency,
    @Size(max = 255) String description
) {
}