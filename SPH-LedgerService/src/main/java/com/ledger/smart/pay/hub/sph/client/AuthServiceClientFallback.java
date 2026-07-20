package com.ledger.smart.pay.hub.sph.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AuthServiceClientFallback implements AuthServiceClient {

    @Override
    public UserResponse getUser(String userId) {
        log.warn("Auth service unavailable, fallback for user: {}", userId);
        return new UserResponse(userId, "UNKNOWN", "UNKNOWN", false);
    }
}