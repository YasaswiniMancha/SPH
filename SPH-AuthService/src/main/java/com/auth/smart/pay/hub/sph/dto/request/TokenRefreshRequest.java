package com.auth.smart.pay.hub.sph.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TokenRefreshRequest(
    @NotBlank(message = "Refresh token is required")
    String refreshToken
) {}