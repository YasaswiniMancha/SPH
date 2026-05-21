package com.auth.smart.pay.hub.sph.dto.response;

import java.time.Instant;
import java.util.Set;

public record UserResponse(
    String id,
    String username,
    String email,
    boolean enabled,
    Instant createdAt,
    Instant lastLoginAt,
    Set<String> roles
) {}