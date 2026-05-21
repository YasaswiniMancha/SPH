package com.auth.smart.pay.hub.sph.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${auth.jwt.secret}")
    private String jwtSecret;

    @Value("${auth.jwt.access-token-expiry-ms:900000}") // 15 minutes
    private int accessTokenExpiryMs;

    @Value("${auth.jwt.refresh-token-expiry-ms:604800000}") // 7 days
    private int refreshTokenExpiryMs;

    @Value("${auth.jwt.issuer:SmartPayHub}")
    private String issuer;

    /**
     * Generate JWT access token from Authentication
     */
    @Timed(value = "auth.token.generate.access", description = "Time taken to generate access token")
    public String generateAccessToken(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        if (userDetails == null) {
            throw new IllegalArgumentException("User details cannot be null");
        }
        return buildToken(userDetails);
    }

    /**
     * Generate JWT access token from username and roles
     */
    @Timed(value = "auth.token.generate.access.roles", description = "Time taken to generate access token with roles")
    public String generateAccessToken(String username, String userId, java.util.Set<String> roles) {
        return buildTokenWithRoles(username, userId, roles);
    }

    /**
     * Generate JWT refresh token
     */
    @Timed(value = "auth.token.generate.refresh", description = "Time taken to generate refresh token")
    public String generateRefreshToken(String username, String userId) {
        return buildRefreshToken(username, userId);
    }

    /**
     * Build access token with user details and roles
     */
    private String buildToken(CustomUserDetails userDetails) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpiryMs);

        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("userId", userDetails.getId())
                .claim("email", userDetails.getEmail())
                .claim("roles", userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList()))
                .claim("permissions", userDetails.getPermissions())
                .claim("tokenType", "ACCESS")
                .issuer(issuer)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * Build access token with roles
     */
    private String buildTokenWithRoles(String username, String userId, java.util.Set<String> roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpiryMs);

        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("roles", roles)
                .claim("tokenType", "ACCESS")
                .issuer(issuer)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * Build refresh token without roles/permissions
     */
    private String buildRefreshToken(String username, String userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpiryMs);

        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("tokenType", "REFRESH")
                .issuer(issuer)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * Get username from token
     */
    @Timed(value = "auth.token.extract.username", description = "Time taken to extract username from token")
    public String getUsernameFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.getSubject();
    }

    /**
     * Get userId from token
     */
    @Timed(value = "auth.token.extract.userid", description = "Time taken to extract userId from token")
    public String getUserIdFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("userId", String.class);
    }

    /**
     * Get all claims from token
     */
    private Claims getAllClaimsFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Validate JWT token
     */
    @Timed(value = "auth.token.validate", description = "Time taken to validate token")
    public boolean validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException ex) {
            log.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Get token expiry time
     */
    @Timed(value = "auth.token.expiry", description = "Time taken to get token expiry")
    public long getTokenExpiry(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.getExpiration().getTime();
    }

    /**
     * Get specific claim from token
     */
    public <T> T getClaimFromToken(String token, String claim, Class<T> claimType) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get(claim, claimType);
    }
}