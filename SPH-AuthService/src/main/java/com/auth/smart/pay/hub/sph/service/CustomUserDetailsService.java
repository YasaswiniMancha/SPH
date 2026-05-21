package com.auth.smart.pay.hub.sph.service;

import com.auth.smart.pay.hub.sph.entity.User;
import com.auth.smart.pay.hub.sph.repository.UserRepository;
import com.auth.smart.pay.hub.sph.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {
    
    private final UserRepository userRepository;
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> {
                log.warn("User not found: {}", username);
                return new UsernameNotFoundException("User not found: " + username);
            });
        
        return new CustomUserDetails(user);
    }
    
    public UserDetails loadUserByEmail(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> {
                log.warn("User not found by email: {}", email);
                return new UsernameNotFoundException("User not found: " + email);
            });
        
        return new CustomUserDetails(user);
    }
}