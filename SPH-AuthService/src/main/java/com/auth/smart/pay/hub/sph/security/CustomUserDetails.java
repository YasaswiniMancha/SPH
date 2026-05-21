package com.auth.smart.pay.hub.sph.security;

import com.auth.smart.pay.hub.sph.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;
import java.util.stream.Collectors;

public class CustomUserDetails implements UserDetails {
    
    private final String id;
    private final String username;
    private final String email;
    private final String password;
    private final boolean enabled;
    private final boolean accountNonExpired;
    private final boolean accountNonLocked;
    private final boolean credentialsNonExpired;
    private final Collection<? extends GrantedAuthority> authorities;
    private final Set<String> permissions;
    
    public CustomUserDetails(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.password = user.getPasswordHash();
        this.enabled = user.isEnabled();
        this.accountNonExpired = user.isAccountNonExpired();
        this.accountNonLocked = user.isAccountNonLocked();
        this.credentialsNonExpired = user.isCredentialsNonExpired();
        
        // Build authorities from roles
        this.authorities = user.getRoles().stream()
            .flatMap(role -> {
                List<GrantedAuthority> roleAuth = new ArrayList<>();
                roleAuth.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
                
                // Add permissions
                role.getPermissions().forEach(permission ->
                    roleAuth.add(new SimpleGrantedAuthority(permission.getCode()))
                );
                
                return roleAuth.stream();
            })
            .collect(Collectors.toSet());
        
        // Build permissions set
        this.permissions = user.getRoles().stream()
            .flatMap(role -> role.getPermissions().stream())
            .map(p -> p.getCode())
            .collect(Collectors.toSet());
    }
    
    public String getId() {
        return id;
    }
    
    public String getEmail() {
        return email;
    }
    
    public Set<String> getPermissions() {
        return permissions;
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }
    
    @Override
    public String getPassword() {
        return password;
    }
    
    @Override
    public String getUsername() {
        return username;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
}