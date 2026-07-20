package com.common.smart.pay.hub.sph.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Correlation ID Filter for distributed logging and request tracing.
 * Generates a unique ID for each request and stores it in MDC (Mapped Diagnostic Context)
 * for centralized log correlation across microservices.
 *
 * Features:
 * - Automatically generates correlation ID if not present
 * - Propagates correlation ID to response headers
 * - Enables log aggregation and tracing in ELK/Splunk/DataDog
 * - Supports distributed tracing patterns
 */
@Component
@Slf4j
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String MDC_CORRELATION_KEY = "correlationId";
    private static final String MDC_REQUEST_ID_KEY = "requestId";
    private static final String MDC_USER_KEY = "userId";
    private static final String MDC_SERVICE_KEY = "service";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // Get or generate correlation ID
            String correlationId = extractCorrelationId(request);
            String requestId = UUID.randomUUID().toString();

            // Store in MDC for logging
            MDC.put(MDC_CORRELATION_KEY, correlationId);
            MDC.put(MDC_REQUEST_ID_KEY, requestId);

            // Extract user context if available from Authorization header
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                MDC.put(MDC_USER_KEY, extractUserFromToken(authHeader));
            }

            // Add service name from context
            String serviceName = System.getenv("SERVICE_NAME");
            if (serviceName != null) {
                MDC.put(MDC_SERVICE_KEY, serviceName);
            }

            // Add headers to response
            response.setHeader(CORRELATION_ID_HEADER, correlationId);
            response.setHeader(REQUEST_ID_HEADER, requestId);
            response.setHeader("X-Service-Name", serviceName != null ? serviceName : "unknown");

            log.debug("Request started - Method: {}, Path: {}, Correlation-ID: {}, Request-ID: {}",
                    request.getMethod(), request.getRequestURI(), correlationId, requestId);

            filterChain.doFilter(request, response);

            log.debug("Request completed - Status: {}, Correlation-ID: {}",
                    response.getStatus(), correlationId);

        } finally {
            // Clean up MDC
            MDC.remove(MDC_CORRELATION_KEY);
            MDC.remove(MDC_REQUEST_ID_KEY);
            MDC.remove(MDC_USER_KEY);
            MDC.remove(MDC_SERVICE_KEY);
        }
    }

    /**
     * Extract correlation ID from request or generate new one
     */
    private String extractCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        return correlationId;
    }

    /**
     * Extract user information from JWT token (simplified version)
     * In production, use JWT decoder to extract claims
     */
    private String extractUserFromToken(String authHeader) {
        try {
            // This is a placeholder - actual implementation should decode JWT
            // For now, return a generic identifier
            return "user";
        } catch (Exception e) {
            return "unknown";
        }
    }
}

