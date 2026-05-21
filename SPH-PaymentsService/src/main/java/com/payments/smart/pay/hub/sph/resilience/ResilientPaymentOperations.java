package com.payments.smart.pay.hub.sph.resilience;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.micrometer.core.annotation.Timed;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import com.payments.smart.pay.hub.sph.entity.Payment;
import com.payments.smart.pay.hub.sph.exceptions.PaymentServiceException;
import com.payments.smart.pay.hub.sph.repository.PaymentRepository;

/**
 * Resilient wrapper for payment database operations with Resilience4j patterns:
 * - Circuit Breaker: Prevents cascading failures
 * - Retry: Handles transient failures
 * - Time Limiter: Prevents hanging requests
 * - Metrics: Tracks operation success/failure rates
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ResilientPaymentOperations {

	private final PaymentRepository paymentRepository;

	@CircuitBreaker(name = "paymentDB", fallbackMethod = "findByIdFallback")
	@Retry(name = "paymentDB")
	@TimeLimiter(name = "paymentDB")
	@Timed(value = "payment.operation.find", description = "Time taken to find payment by ID")
	public CompletableFuture<Optional<Payment>> findPaymentById(UUID paymentId) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				log.debug("Fetching payment with ID: {}", paymentId);
				return paymentRepository.findById(paymentId);
			} catch (DataAccessException ex) {
				log.error("Database error while fetching payment: {}", paymentId, ex);
				throw new PaymentServiceException("Failed to fetch payment: " + paymentId, ex);
			}
		});
	}

	@CircuitBreaker(name = "paymentDB", fallbackMethod = "findWithLockFallback")
	@Retry(name = "paymentDB")
	@TimeLimiter(name = "paymentDB")
	@Timed(value = "payment.operation.find.lock", description = "Time taken to find payment with lock")
	public CompletableFuture<Optional<Payment>> findPaymentWithLock(UUID paymentId) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				log.debug("Fetching payment with pessimistic lock: {}", paymentId);
				return paymentRepository.findWithLockById(paymentId);
			} catch (OptimisticLockingFailureException ex) {
				log.warn("Optimistic lock conflict while fetching payment: {}", paymentId);
				throw new PaymentServiceException("Payment was modified concurrently: " + paymentId);
			} catch (DataAccessException ex) {
				log.error("Database error while fetching payment with lock: {}", paymentId, ex);
				throw new PaymentServiceException("Failed to fetch payment with lock: " + paymentId, ex);
			}
		});
	}

	@CircuitBreaker(name = "paymentDB", fallbackMethod = "savePaymentFallback")
	@Retry(name = "paymentDB")
	@TimeLimiter(name = "paymentDB")
	@Timed(value = "payment.operation.save", description = "Time taken to save payment")
	public CompletableFuture<Payment> savePayment(Payment payment) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				log.debug("Saving payment: {}", payment.getId());
				return paymentRepository.save(payment);
			} catch (OptimisticLockingFailureException ex) {
				log.warn("Optimistic lock conflict while saving payment: {}", payment.getId());
				throw new PaymentServiceException("Payment was modified concurrently", ex);
			} catch (DataAccessException ex) {
				log.error("Database error while saving payment", ex);
				throw new PaymentServiceException("Failed to save payment", ex);
			}
		});
	}

	public CompletableFuture<Optional<Payment>> findByIdFallback(UUID paymentId, Exception ex) {
		log.warn("Circuit breaker fallback for findPaymentById: {}", paymentId, ex);
		throw new PaymentServiceException("Payment service temporarily unavailable", ex);
	}

	public CompletableFuture<Optional<Payment>> findWithLockFallback(UUID paymentId, Exception ex) {
		log.warn("Circuit breaker fallback for findPaymentWithLock: {}", paymentId, ex);
		throw new PaymentServiceException("Payment service temporarily unavailable", ex);
	}

	public CompletableFuture<Payment> savePaymentFallback(Payment payment, Exception ex) {
		log.warn("Circuit breaker fallback for savePayment: {}", payment.getId(), ex);
		throw new PaymentServiceException("Payment service temporarily unavailable", ex);
	}
}