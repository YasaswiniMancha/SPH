package com.payments.smart.pay.hub.sph.resilience;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.micrometer.core.annotation.Timed;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import com.payments.smart.pay.hub.sph.entity.PaymentTransaction;
import com.payments.smart.pay.hub.sph.exceptions.PaymentServiceException;
import com.payments.smart.pay.hub.sph.repository.PaymentTransactionRepository;

/**
 * Resilient wrapper for payment transaction database operations
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ResilientTransactionOperations {

	private final PaymentTransactionRepository transactionRepository;

	@CircuitBreaker(name = "paymentDB", fallbackMethod = "saveTransactionFallback")
	@Retry(name = "paymentDB")
	@TimeLimiter(name = "paymentDB")
	@Timed(value = "payment.transaction.save", description = "Time taken to save payment transaction")
	public CompletableFuture<PaymentTransaction> saveTransaction(PaymentTransaction transaction) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				log.debug("Saving payment transaction: {}", transaction.getId());
				return transactionRepository.save(transaction);
			} catch (DataAccessException ex) {
				log.error("Database error while saving transaction", ex);
				throw new PaymentServiceException("Failed to save transaction", ex);
			}
		});
	}

	public CompletableFuture<PaymentTransaction> saveTransactionFallback(PaymentTransaction transaction, Exception ex) {
		log.warn("Circuit breaker fallback for saveTransaction", ex);
		throw new PaymentServiceException("Payment service temporarily unavailable", ex);
	}
}