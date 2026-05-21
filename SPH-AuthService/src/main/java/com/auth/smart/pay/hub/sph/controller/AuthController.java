package com.auth.smart.pay.hub.sph.controller;

import com.auth.smart.pay.hub.sph.dto.request.AuthRequest;
import com.auth.smart.pay.hub.sph.dto.request.RegisterRequest;
import com.auth.smart.pay.hub.sph.dto.request.TokenRefreshRequest;
import com.auth.smart.pay.hub.sph.dto.request.ValidateTokenRequest;
import com.auth.smart.pay.hub.sph.dto.response.AuthResponse;
import com.auth.smart.pay.hub.sph.security.JwtTokenProvider;
import com.auth.smart.pay.hub.sph.service.AuthService;


import com.auth.smart.pay.hub.sph.service.AuthenticationRateLimiter;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    private final AuthenticationRateLimiter authenticationRateLimiter;

    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = getClientIp(httpRequest); // Get client IP for rate limiting to prevent abuse of registration endpoint, a strategy used to control the volume of incoming traffic by restricting the number of API requests a client can make within a specific timeframe, when a client exceeds the defined limit, the server responds with a 429 Too Many Requests status, indicating that the client should slow down their request rate. This helps to protect the server from being overwhelmed by excessive requests and ensures fair usage of resources among all clients.
        if (!authenticationRateLimiter.checkRegisterRateLimit(clientIp)) { // Check if the number of registration attempts from the given client IP exceeds the defined limit
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(null);
        }
        AuthResponse response = authService.register(request); // Call the register method of the AuthService to handle the registration logic and return an AuthResponse containing the result of the registration process, such as a success message or an authentication token if registration is successful.
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody AuthRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = getClientIp(httpRequest);
        if (!authenticationRateLimiter.checkLoginRateLimit(clientIp)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(null);
        }

        AuthResponse response = authService.login(request, clientIp);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate")
    public ResponseEntity<Boolean> validateToken(@Valid @RequestBody ValidateTokenRequest request) {
        boolean isValid = authService.validateToken(request.token());
        return ResponseEntity.ok(isValid);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().build();
        }

        token = token.substring(7);
        try {
            String userId = jwtTokenProvider.getUserIdFromToken(token);
            authService.logout(token, userId);
            return ResponseEntity.ok().build();
        } catch (JwtException ex) {
            return ResponseEntity.status(401).build();  // Invalid token
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0];
        }
        return request.getRemoteAddr();
    }
}