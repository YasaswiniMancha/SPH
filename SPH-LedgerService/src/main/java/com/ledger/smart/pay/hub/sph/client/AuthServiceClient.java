package com.ledger.smart.pay.hub.sph.client;

import com.ledger.smart.pay.hub.sph.client.AuthServiceClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "auth-service",
    url = "${auth-service.url:http://localhost:8080}",
    fallback = AuthServiceClientFallback.class
)
public interface AuthServiceClient {

    @GetMapping("/api/v1/users/{userId}")
    UserResponse getUser(@PathVariable String userId);
}

record UserResponse(
        String id,
        String username,
        String email,
        boolean enabled
) {}