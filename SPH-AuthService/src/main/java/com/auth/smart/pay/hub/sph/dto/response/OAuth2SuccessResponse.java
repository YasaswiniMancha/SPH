package com.auth.smart.pay.hub.sph.dto.response;

import java.time.Instant;
import java.util.Set;

public record OAuth2SuccessResponse(
    String userId,
    String username,
    String email,
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn,
    Set<String> roles,
    Set<String> permissions,
    Instant issuedAt
) {}