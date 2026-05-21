package com.auth.smart.pay.hub.sph.controller;

import com.auth.smart.pay.hub.sph.dto.response.OAuth2SuccessResponse;
import com.auth.smart.pay.hub.sph.service.AuthService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/oauth2")
@RequiredArgsConstructor
public class OAuth2Controller {

    private final AuthService authService;

    @GetMapping("/success")
    public ResponseEntity<OAuth2SuccessResponse> oauth2Success(@AuthenticationPrincipal OAuth2User oauth2User) {
        // Handle OAuth2 success, generate tokens
        // Assuming you extend AuthService for OAuth2
        return ResponseEntity.ok(null); // Placeholder
    }
}