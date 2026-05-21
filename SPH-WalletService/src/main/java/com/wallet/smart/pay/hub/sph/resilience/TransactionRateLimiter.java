package com.wallet.smart.pay.hub.sph.resilience;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.wallet.smart.pay.hub.sph.exception.WalletServiceException;

/**
 * Rate limiting for wallet transactions to support high-traffic scenarios.
 * 
 * 10,000 transactions per day = ~0.12 transactions per second on average
 * However, the system should handle 1000 transactions per minute (16.67 TPS) burst traffic
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionRateLimiter {

    private final RateLimiterRegistry rateLimiterRegistry;
    
    private static final String TRANSACTIONS_LIMITER = "walletTransactions";

    @io.github.resilience4j.ratelimiter.annotation.RateLimiter(name = TRANSACTIONS_LIMITER)
    @Timed(value = "wallet.ratelimit.check", description = "Rate limit check for wallet transactions")
    public void checkTransactionRateLimit() {
        log.debug("Transaction rate limit check passed");
    }

    public void acquirePermission() {
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(TRANSACTIONS_LIMITER);
        boolean permitted = rateLimiter.acquirePermission();
        
        if (!permitted) {
            long waitDuration = rateLimiter.reservePermission();
            if (waitDuration > 0) {
                throw new WalletServiceException("Rate limit exceeded. Please retry after " + waitDuration + " ms");
            }
            throw new WalletServiceException("Transaction rate limit exceeded. Service temporarily overloaded");
        }
        
        log.debug("Transaction rate limit acquired. Available permissions: {}", 
            rateLimiter.getMetrics().getAvailablePermissions());
    }

    public boolean tryAcquirePermission() {
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(TRANSACTIONS_LIMITER);
        try {
            rateLimiter.acquirePermission();
            return true;
        } catch (Exception ex) {
            log.warn("Rate limit check failed", ex);
            return false;
        }
    }

    public RateLimiter.Metrics getMetrics() {
        return rateLimiterRegistry.rateLimiter(TRANSACTIONS_LIMITER).getMetrics();
    }
}

