package com.auth.smart.pay.hub.sph.service;

import com.auth.smart.pay.hub.sph.entity.User;
import com.auth.smart.pay.hub.sph.entity.Role;
import com.auth.smart.pay.hub.sph.repository.UserRepository;
import com.auth.smart.pay.hub.sph.repository.RoleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        try {
            // Extract user info from OAuth2 provider
            String email = oauth2User.getAttribute("email");

            if (email == null || email.isEmpty()) {
                throw new OAuth2AuthenticationException("Email not provided by OAuth2 provider");
            }

            // Find or create user
            User user = userRepository.findByEmail(email).orElseGet(() -> {
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setUsername(email.split("@")[0]); // Generate username from email
                newUser.setEnabled(true);
                newUser.setCreatedAt(Instant.now());

                // Assign default USER role
                Role userRole = roleRepository.findByName("USER")
                        .orElseGet(() -> {
                            Role role = new Role();
                            role.setName("USER");
                            role.setDescription("Default user role");
                            role.setPermissions(new HashSet<>());
                            return roleRepository.save(role);
                        });

                Set<Role> roles = new HashSet<>();
                roles.add(userRole);
                newUser.setRoles(roles);

                User savedUser = userRepository.save(newUser);
                log.info("New OAuth2 user created: {}", email);
                return savedUser;
            });

            // Update last login
            user.setLastLoginAt(Instant.now());
            userRepository.save(user);

            return new CustomOAuth2User(oauth2User, user);
        } catch (OAuth2AuthenticationException e) {
            log.error("OAuth2 authentication error: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error loading OAuth2 user: {}", e.getMessage(), e);
            throw new OAuth2AuthenticationException("Failed to load OAuth2 user");
        }
    }
}