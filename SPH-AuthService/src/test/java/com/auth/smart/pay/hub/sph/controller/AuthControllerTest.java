package com.auth.smart.pay.hub.sph.controller;

import com.auth.smart.pay.hub.sph.dto.request.AuthRequest;
import com.auth.smart.pay.hub.sph.dto.request.RegisterRequest;
import com.auth.smart.pay.hub.sph.dto.request.TokenRefreshRequest;
import com.auth.smart.pay.hub.sph.dto.request.ValidateTokenRequest;
import com.auth.smart.pay.hub.sph.dto.response.AuthResponse;
import com.auth.smart.pay.hub.sph.service.AuthenticationRateLimiter;
import com.auth.smart.pay.hub.sph.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private AuthenticationRateLimiter authenticationRateLimiter;

    @InjectMocks
    private AuthController authController;

    @Test
    void registerSuccess() {
        RegisterRequest req = new RegisterRequest("user123", "u@example.com", "password12345", "password12345");
        when(authenticationRateLimiter.checkRegisterRateLimit(anyString())).thenReturn(true);
        when(authService.register(req)).thenReturn(new AuthResponse("id", "user123", "u@example.com", "tok", "ref", "Bearer", 900000, java.util.Set.of("USER"), java.util.Set.of(), java.time.Instant.now()));

        HttpServletRequest servletReq = mock(HttpServletRequest.class);
        when(servletReq.getHeader("X-Forwarded-For")).thenReturn(null);
        when(servletReq.getRemoteAddr()).thenReturn("127.0.0.1");

        var resp = authController.register(req, servletReq);
        assertEquals(201, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        verify(authService).register(req);
    }

    @Test
    void registerRateLimited() {
        RegisterRequest req = new RegisterRequest("user123", "u@example.com", "password12345", "password12345");
        when(authenticationRateLimiter.checkRegisterRateLimit(anyString())).thenReturn(false);
        HttpServletRequest servletReq = mock(HttpServletRequest.class);
        when(servletReq.getHeader("X-Forwarded-For")).thenReturn(null);
        when(servletReq.getRemoteAddr()).thenReturn("127.0.0.1");

        var resp = authController.register(req, servletReq);
        assertEquals(429, resp.getStatusCode().value());
        verify(authService, never()).register(any());
    }

    @Test
    void loginSuccess() {
        AuthRequest req = new AuthRequest("user123", "password12345", "127.0.0.1");
        when(authenticationRateLimiter.checkLoginRateLimit(anyString())).thenReturn(true);
        when(authService.login(req, "127.0.0.1")).thenReturn(new AuthResponse("id","user123","u@example.com","tok","ref","Bearer",900000,java.util.Set.of("USER"),java.util.Set.of(), java.time.Instant.now()));

        HttpServletRequest servletReq = mock(HttpServletRequest.class);
        when(servletReq.getHeader("X-Forwarded-For")).thenReturn(null);
        when(servletReq.getRemoteAddr()).thenReturn("127.0.0.1");

        var resp = authController.login(req, servletReq);
        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
    }

    @Test
    void loginRateLimited() {
        AuthRequest req = new AuthRequest("user123", "password12345", "127.0.0.1");
        when(authenticationRateLimiter.checkLoginRateLimit(anyString())).thenReturn(false);

        HttpServletRequest servletReq = mock(HttpServletRequest.class);
        when(servletReq.getHeader("X-Forwarded-For")).thenReturn(null);
        when(servletReq.getRemoteAddr()).thenReturn("127.0.0.1");

        var resp = authController.login(req, servletReq);
        assertEquals(429, resp.getStatusCode().value());
        verify(authService, never()).login(any(), anyString());
    }

    @Test
    void validateToken() {
        ValidateTokenRequest req = new ValidateTokenRequest("valid-token");
        when(authService.validateToken("valid-token")).thenReturn(true);

        var resp = authController.validateToken(req);
        assertTrue(resp.getBody());
    }

    @Test
    void logoutInvalidToken() {
        HttpServletRequest servletReq = mock(HttpServletRequest.class);
        when(servletReq.getHeader("Authorization")).thenReturn(null);

        var resp = authController.logout(servletReq);
        assertEquals(400, resp.getStatusCode().value());
    }

    @Test
    void logoutSuccess() {
        HttpServletRequest servletReq = mock(HttpServletRequest.class);
        when(servletReq.getHeader("Authorization")).thenReturn("Bearer valid.jwt.token");

        var resp = authController.logout(servletReq);
        assertEquals(200, resp.getStatusCode().value());
    }
}

