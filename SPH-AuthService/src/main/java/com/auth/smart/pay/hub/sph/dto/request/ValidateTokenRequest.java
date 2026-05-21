package com.auth.smart.pay.hub.sph.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ValidateTokenRequest(
    @NotBlank(message = "Token is required")
    String token
) {}