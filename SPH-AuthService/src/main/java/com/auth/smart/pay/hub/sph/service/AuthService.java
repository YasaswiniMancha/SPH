package com.auth.smart.pay.hub.sph.service;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.auth.smart.pay.hub.sph.entity.Permission;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auth.smart.pay.hub.sph.dto.request.AuthRequest;
import com.auth.smart.pay.hub.sph.dto.request.RegisterRequest;
import com.auth.smart.pay.hub.sph.dto.request.TokenRefreshRequest;
import com.auth.smart.pay.hub.sph.dto.response.AuthResponse;
import com.auth.smart.pay.hub.sph.entity.Role;
import com.auth.smart.pay.hub.sph.entity.User;
import com.auth.smart.pay.hub.sph.exceptions.BadRequestException;
import com.auth.smart.pay.hub.sph.exceptions.NotFoundException;
import com.auth.smart.pay.hub.sph.repository.RoleRepository;
import com.auth.smart.pay.hub.sph.repository.UserRepository;
import com.auth.smart.pay.hub.sph.security.JwtTokenProvider;

import io.micrometer.core.annotation.Timed;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserAuthEventPublisher authEventPublisher;
    private final RedisSessionService redisSessionService;
    private final AuthenticationRateLimiter authenticationRateLimiter;

    @Transactional
    @CacheEvict(value = "user-auth", key = "#registerRequest.username()", allEntries = false) // Evict(means remove) cache for this username on registration to ensure fresh data, a caching strategy that removes specific entries from the cache when certain operations occur, such as updating or deleting data, to ensure that subsequent/consecutive requests retrieve the most up-to-date information from the underlying data source rather than serving stale data from the cache.
    //If a user registers with a username that was previously used (or if there is any pre-existing temporary data), this line guarantees they start with a clean slate.
    @Timed(value = "auth.register", description = "User registration time") // Measure the time taken for the user registration process, which can be used for monitoring and performance analysis. This annotation allows you to track how long the registration method takes to execute, providing insights into the performance of the registration functionality and helping identify potential bottlenecks or areas for optimization.
    @CircuitBreaker(name = "authServiceCB", fallbackMethod = "registerFallback") // Implement a circuit breaker pattern to handle failures gracefully during the registration process, which can help prevent cascading(happens in a series of stages) failures and improve the resilience of the application. If the registration process fails repeatedly (e.g., due to database issues or external service failures), the circuit breaker will open and redirect to the specified fallback method, allowing the system to degrade gracefully instead of crashing or becoming unresponsive.
    @Retry(name = "authServiceRetry", fallbackMethod = "registerFallback")  // Implement a retry mechanism to automatically retry the registration process in case of transient(temporary) failures, such as temporary network issues or database timeouts, which can improve the chances of successful registration without requiring manual intervention. If the registration process encounters a failure, it will automatically retry according to the defined retry policy, and if it continues to fail after the maximum number of retries, it will redirect to the specified fallback method.
    public AuthResponse register(RegisterRequest registerRequest) {
        try{
            log.info("Registering new user: {}", registerRequest.username());

            // Validate input
            if (!registerRequest.password().equals(registerRequest.confirmPassword())) {
                throw new BadRequestException("Passwords do not match");
            }

            if (registerRequest.username().length() < 3 || registerRequest.username().length() > 50) {
                throw new BadRequestException("Username must be between 3 and 50 characters");
            }

            // Check existence asynchronously
            if (userRepository.existsByUsername(registerRequest.username())) {
                throw new BadRequestException("Username already exists");
            }

            if (userRepository.existsByEmail(registerRequest.email())) {
                throw new BadRequestException("Email already registered");
            }

            // Create user
            User user = new User();
            user.setUsername(registerRequest.username());
            user.setEmail(registerRequest.email());
            user.setPasswordHash(passwordEncoder.encode(registerRequest.password()));
            user.setEnabled(true);
            user.setCreatedAt(Instant.now());

            // Assign default USER role
            Set<Role> roles = new HashSet<>();
            Role userRole = roleRepository.findByName("USER")
                    .orElseThrow(() -> new NotFoundException("Default USER role not found"));
            roles.add(userRole);
            user.setRoles(roles);

            user = userRepository.save(user);
            log.info("User registered: {}", user.getId());

            // Async: Publish event to Kafka
            publishRegistrationEventAsync(user);

            // Generate tokens
            Set<String> roleNames = user.getRoles().stream()
                    .map(Role::getName)
                    .collect(Collectors.toSet());

            String accessToken = jwtTokenProvider.generateAccessToken(user.getUsername(), user.getId(), roleNames);
            String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername(), user.getId());

            // Cache permissions
            Set<String> permissions = cacheUserPermissions(user);

            // Create session in Redis
            String sessionId = UUID.randomUUID().toString();
            redisSessionService.createSession(sessionId, user.getId(), accessToken, 900);

            return new AuthResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    accessToken,
                    refreshToken,
                    "Bearer",
                    900000,
                    roleNames,
                    permissions,
                    Instant.now()
            );
        } catch (DataIntegrityViolationException ex) {
            throw new BadRequestException("Username or email already exists");
        }
    }

    public AuthResponse registerFallback(RegisterRequest registerRequest, Exception ex) {
        log.error("Registration fallback triggered: {}", ex.getMessage());
        throw new BadRequestException("Registration temporarily unavailable. Please try again later.");
    }//used in registration process to handle failures gracefully, such as database issues or external service failures, by providing a fallback response instead of crashing the application.

    @Transactional
    @Timed(value = "auth.login", description = "User login time")
    @CircuitBreaker(name = "authServiceCB", fallbackMethod = "loginFallback")
    @Retry(name = "authServiceRetry", fallbackMethod = "loginFallback")
    public AuthResponse login(AuthRequest authRequest, String clientIp) {
        log.info("Login attempt from IP: {} for user: {}", clientIp, authRequest.username());

        // Rate limiting check - use clientIp parameter, not from authRequest
        if (!authenticationRateLimiter.checkLoginRateLimit(clientIp)) {
            throw new BadRequestException("Too many login attempts. Please try again later.");
        }

        try {
            // Authenticate
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authRequest.username(),
                            authRequest.password()
                    )
            );

            User user = userRepository.findByUsername(authRequest.username())
                    .orElseThrow(() -> new NotFoundException("User not found"));

            if (!user.isEnabled()) {
                throw new BadRequestException("User account is disabled");
            }

            // Update last login async
            updateLastLoginAsync(user);

            log.info("User logged in: {}", authRequest.username());

            // Publish event async
            authEventPublisher.publishUserLogin(user.getId(), user.getUsername());

            // Generate tokens
            String accessToken = jwtTokenProvider.generateAccessToken(authentication);
            Set<String> roleNames = user.getRoles().stream()
                    .map(Role::getName)
                    .collect(Collectors.toSet());
            String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername(), user.getId());

            // Get cached permissions
            Set<String> permissions = cacheUserPermissions(user);

            // Create session
            String sessionId = UUID.randomUUID().toString();
            redisSessionService.createSession(sessionId, user.getId(), accessToken, 900);

            return new AuthResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    accessToken,
                    refreshToken,
                    "Bearer",
                    900000,
                    roleNames,
                    permissions,
                    Instant.now()
            );
        } catch (AuthenticationException ex) {
            log.warn("Authentication failed: {}", authRequest.username());
            throw new BadRequestException("Invalid username or password");
        }
    }

    public AuthResponse loginFallback(AuthRequest authRequest, String clientIp, Exception ex) {
        log.error("Login fallback triggered: {}", ex.getMessage());
        throw new BadRequestException("Login temporarily unavailable. Please try again later.");
    }

    @Transactional
    @Timed(value = "auth.refresh", description = "Token refresh time")
    public AuthResponse refreshToken(TokenRefreshRequest refreshRequest) {
        log.debug("Refreshing token");

        if (!jwtTokenProvider.validateToken(refreshRequest.refreshToken())) {
            throw new BadRequestException("Invalid or expired refresh token");
        }

        String username = jwtTokenProvider.getUsernameFromToken(refreshRequest.refreshToken());
        String userId = jwtTokenProvider.getUserIdFromToken(refreshRequest.refreshToken());

        // Check cache first
        Optional<User> cachedUser = getUserById(userId);
        User user = cachedUser.orElseGet(() ->
                userRepository.findById(userId)
                        .orElseThrow(() -> new NotFoundException("User not found"))
        );

        if (!user.getUsername().equals(username)) {
            throw new BadRequestException("Token mismatch");
        }

        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        String accessToken = jwtTokenProvider.generateAccessToken(username, userId, roleNames);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(username, userId);

        Set<String> permissions = cacheUserPermissions(user);

        // Create new session
        String sessionId = UUID.randomUUID().toString();
        redisSessionService.createSession(sessionId, userId, accessToken, 900);

        return new AuthResponse(
                userId,
                username,
                user.getEmail(),
                accessToken,
                newRefreshToken,
                "Bearer",
                900000,
                roleNames,
                permissions,
                Instant.now()
        );
    }

    @Transactional
    @Timed(value = "auth.logout", description = "User logout time")
    public void logout(String token, String userId) {
        log.info("Logout request for user: {}", userId);

        try {
            if (jwtTokenProvider.validateToken(token)) {
                long expiryTime = jwtTokenProvider.getTokenExpiry(token);
                tokenBlacklistService.addToBlacklist(token, expiryTime);
            }

            // Invalidate all sessions async
            invalidateUserSessionsAsync(userId);

            // Publish event async
            authEventPublisher.publishUserLogout(userId);

            log.info("User logged out: {}", userId);
        } catch (Exception ex) {
            log.error("Logout error for user: {}", userId, ex);
            // Don't throw - logout should always succeed
        }
    }

    @Transactional(readOnly = true)
    public boolean validateToken(String token) {
        if (!jwtTokenProvider.validateToken(token)) return false;
        if (tokenBlacklistService.isBlacklisted(token)) return false;
        return true;
    }

    @Cacheable(value = "user", key = "#userId", unless = "#result == null or !#result.isPresent()")
    @Transactional(readOnly = true)
    @Timed(value = "auth.user.get", description = "Get user time")
    public Optional<User> getUserById(String userId) {
        return userRepository.findById(userId);
    }

    @Async("authTokenExecutor")
    @Timed(value = "auth.async.publish.event", description = "Async event publishing")
    protected void publishRegistrationEventAsync(User user) {
        try {
            authEventPublisher.publishUserRegistration(user.getId(), user.getUsername(), user.getEmail());
        } catch (Exception ex) {
            log.error("Failed to publish registration event: {}", ex.getMessage());
        }
    }

    @Async("authTokenExecutor")
    @Timed(value = "auth.async.update.login", description = "Async last login update")
    protected void updateLastLoginAsync(User user) {
        try {
            user.setLastLoginAt(Instant.now());
            userRepository.save(user);
        } catch (Exception ex) {
            log.error("Failed to update last login: {}", ex.getMessage());
        }
    }

    @Async("authTokenExecutor")
    @Timed(value = "auth.async.invalidate.sessions", description = "Async session invalidation")
    protected void invalidateUserSessionsAsync(String userId) {
        try {
            redisSessionService.logoutAllUserSessions(userId);
        } catch (Exception ex) {
            log.error("Failed to invalidate sessions: {}", ex.getMessage());
        }
    }

    private Set<String> cacheUserPermissions(User user) {
        return user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getCode)// .map(Permission::getCode) if using method reference
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    @Timed(value = "auth.token.extract.userid", description = "Extract userId from token")
    public String extractUserIdFromToken(String token) {
        return jwtTokenProvider.getUserIdFromToken(token);
    }
}