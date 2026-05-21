package com.auth.smart.pay.hub.sph.exceptions;

import org.springframework.security.core.AuthenticationException;

public class OAuth2AuthenticationException extends AuthenticationException {
    
    public OAuth2AuthenticationException(String message) {
        super(message);
    }

    public OAuth2AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }

    public OAuth2AuthenticationException(Throwable cause) {
        super("OAuth2 Authentication failed", cause);
    }
}