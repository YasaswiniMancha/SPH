package com.payments.smart.pay.hub.sph.resilience;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.payments.smart.pay.hub.sph.exceptions.PaymentServiceException;

/**
 * Rate limiting for payment transactions to support high-traffic scenarios.
 * 
 * 10,000 transactions per day = ~0.12 transactions per second on average
 * However, the system should handle 1000 transactions per minute (16.67 TPS) burst traffic
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentRateLimiter {

	private final RateLimiterRegistry rateLimiterRegistry;
	
	private static final String TRANSACTIONS_LIMITER = "paymentTransactions";

	@io.github.resilience4j.ratelimiter.annotation.RateLimiter(name = TRANSACTIONS_LIMITER)
	@Timed(value = "payment.ratelimit.check", description = "Rate limit check for payment transactions")
	public void checkTransactionRateLimit() {
		log.debug("Payment transaction rate limit check passed");
	}

	public void acquirePermission() {
		RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(TRANSACTIONS_LIMITER);
		boolean permitted = rateLimiter.acquirePermission();
		
		if (!permitted) {
			long waitDuration = rateLimiter.reservePermission();
			if (waitDuration > 0) {
				throw new PaymentServiceException("Rate limit exceeded. Please retry after " + waitDuration + " ms");
			}
			throw new PaymentServiceException("Payment rate limit exceeded. Service temporarily overloaded");
		}
		
		log.debug("Payment rate limit acquired. Available permissions: {}", 
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