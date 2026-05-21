package com.auth.smart.pay.hub.sph.controller;

import com.auth.smart.pay.hub.sph.dto.request.UpdateUserRequest;
import com.auth.smart.pay.hub.sph.dto.response.UserResponse;
import com.auth.smart.pay.hub.sph.entity.User;
import com.auth.smart.pay.hub.sph.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        // Assuming you add a method in AuthService to get all users
        // For now, placeholder
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<UserResponse> getUser(@PathVariable String userId) {
        Optional<User> user = authService.getUserById(userId);
        if (user.isPresent()) {
            User u = user.get();
            UserResponse response = new UserResponse(
                u.getId(), u.getUsername(), u.getEmail(), u.isEnabled(),
                u.getCreatedAt(), u.getLastLoginAt(),
                u.getRoles().stream().map(r -> r.getName()).collect(java.util.stream.Collectors.toSet())
            );
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<UserResponse> updateUser(@PathVariable String userId, @Valid @RequestBody UpdateUserRequest request) {
        // Implement update logic in AuthService
        return ResponseEntity.ok(null); // Placeholder
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        // Implement delete logic
        return ResponseEntity.noContent().build();
    }
}