package com.auth.smart.pay.hub.sph.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"auth.jwt.secret=01234567890123456789012345678901"})
class JwtTokenProviderTest {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void generateAndValidateRefreshToken() {
        String token = jwtTokenProvider.generateRefreshToken("testuser", "user-123");
        boolean valid = jwtTokenProvider.validateToken(token);
        assertThat(valid).isTrue();
        String username = jwtTokenProvider.getUsernameFromToken(token);
        assertThat(username).isEqualTo("testuser");
    }
}

