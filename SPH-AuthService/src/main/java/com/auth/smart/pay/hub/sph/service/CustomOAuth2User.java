package com.auth.smart.pay.hub.sph.service;

import java.util.Collection;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.auth.smart.pay.hub.sph.entity.User;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class CustomOAuth2User implements OAuth2User {

    private final OAuth2User oauth2User;
    private final User user;

    @Override
    public Map<String, Object> getAttributes() {
        return oauth2User.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .toList();
    }

    @Override
    public String getName() {
        return user.getUsername();
    }

    public String getEmail() {
        return user.getEmail();
    }

    public String getUserId() {
        return user.getId();
    }

    public User getUser() {
        return user;
    }
}