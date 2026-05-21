package com.payments.smart.pay.hub.sph.service;


import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payments.smart.pay.hub.sph.concurrency.PaymentLockManager;
import com.payments.smart.pay.hub.sph.dto.request.CreatePaymentRequest;
import com.payments.smart.pay.hub.sph.dto.request.ProcessPaymentRequest;
import com.payments.smart.pay.hub.sph.dto.request.RefundPaymentRequest;
import com.payments.smart.pay.hub.sph.dto.response.PaymentResponse;
import com.payments.smart.pay.hub.sph.dto.response.PaymentTransactionResponse;
import com.payments.smart.pay.hub.sph.entity.IdempotencyRecord;
import com.payments.smart.pay.hub.sph.entity.Payment;
import com.payments.smart.pay.hub.sph.entity.PaymentStatus;
import com.payments.smart.pay.hub.sph.entity.PaymentTransaction;
import com.payments.smart.pay.hub.sph.entity.TransactionType;
import com.payments.smart.pay.hub.sph.event.PaymentTransactionEvent;
import com.payments.smart.pay.hub.sph.event.publisher.PaymentEventPublisher;
import com.payments.smart.pay.hub.sph.exceptions.BadRequestException;
import com.payments.smart.pay.hub.sph.exceptions.NotFoundException;
import com.payments.smart.pay.hub.sph.exceptions.PaymentServiceException;
import com.payments.smart.pay.hub.sph.repository.IdempotencyRecordRepository;
import com.payments.smart.pay.hub.sph.repository.PaymentRepository;
import com.payments.smart.pay.hub.sph.repository.PaymentTransactionRepository;
import com.payments.smart.pay.hub.sph.resilience.PaymentRateLimiter;
import com.payments.smart.pay.hub.sph.resilience.ResilientPaymentOperations;
import com.payments.smart.pay.hub.sph.resilience.ResilientTransactionOperations;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Enhanced Payment Service with comprehensive resilience patterns:
 * - Circuit breakers for database operations
 * - Retry mechanisms for transient failures
 * - Rate limiting for traffic control
 * - Transactional Outbox Pattern for guaranteed event delivery
 * - Kafka event publishing for downstream services
 * - Optimized transaction handling
 * - Caching strategy for performance
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

	private final PaymentRepository paymentRepository;
	private final PaymentTransactionRepository transactionRepository;
	private final IdempotencyRecordRepository idempotencyRecordRepository;
	private final ResilientPaymentOperations resilientPaymentOps;
	private final ResilientTransactionOperations resilientTransactionOps;
	private final PaymentRateLimiter rateLimiter;
	private final PaymentOutboxService paymentOutboxService;
	private final PaymentEventPublisher paymentEventPublisher;
	private final PaymentLockManager paymentLockManager;

	@Transactional
	@Timed(value = "payment.creation", description = "Time taken to create a payment")
	public PaymentResponse createPayment(CreatePaymentRequest request) {
		log.info("Creating payment for merchant: {} customer: {}", request.merchantId(), request.customerId());
		
		rateLimiter.acquirePermission();
		
		try {
			Payment payment = new Payment();
			payment.setMerchantId(request.merchantId());
			payment.setCustomerId(request.customerId());
			payment.setAmount(request.amount());
			payment.setCurrency(request.currency().toUpperCase());
			payment.setPaymentMethodId(request.paymentMethodId());
			payment.setDescription(request.description());
			payment.setStatus(PaymentStatus.PENDING);
			payment.setReferenceId("PAY-" + UUID.randomUUID());
			
			payment = resilientPaymentOps.savePayment(payment).get();
			log.info("Payment created successfully with ID: {}", payment.getId());
			return toPaymentResponse(payment);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			log.error("Interrupted while creating payment", ex);
			throw new PaymentServiceException("Payment creation interrupted", ex);
		} catch (ExecutionException ex) {
			log.error("Failed to create payment", ex);
			throw new BadRequestException("Failed to create payment: " + ex.getCause().getMessage());
		}
	}

	@Cacheable(value = "payment-status", key = "#paymentId")
	@Transactional(readOnly = true)
	@Timed(value = "payment.fetch", description = "Time taken to fetch payment")
	public PaymentResponse getPayment(UUID paymentId) {
		log.debug("Fetching payment: {}", paymentId);
		
		try {
			Payment payment = resilientPaymentOps.findPaymentById(paymentId)
				.thenApply(opt -> opt.orElseThrow(() -> 
					new NotFoundException("Payment not found: " + paymentId)))
				.get();
			
			return toPaymentResponse(payment);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			log.error("Interrupted while fetching payment: {}", paymentId, ex);
			throw new PaymentServiceException("Payment fetch interrupted", ex);
		} catch (ExecutionException ex) {
			log.error("Failed to fetch payment: {}", paymentId, ex);
			throw new PaymentServiceException("Failed to fetch payment: " + paymentId, ex.getCause());
		}
	}

	@Transactional
	@CacheEvict(value = "payment-status", key = "#request.paymentId()")
	@Timed(value = "payment.process", description = "Time taken to process payment")
	public PaymentTransactionResponse processPayment(ProcessPaymentRequest request, String idempotencyKey) {
		log.info("Processing payment: {}", request.paymentId());
		
		rateLimiter.acquirePermission();
		
		if (idempotencyKey != null && !idempotencyKey.isBlank()) {
			PaymentTransactionResponse existing = getExistingIdempotentTransaction(idempotencyKey);
			if (existing != null) {
				log.debug("Idempotent transaction found for key: {}. Returning cached response.", idempotencyKey);
				return existing;
			}
		}

		ReentrantLock lock = paymentLockManager.getLock(request.paymentId());
		lock.lock();
		try {
			Payment payment = resilientPaymentOps.findPaymentWithLock(request.paymentId())
				.thenApply(opt -> opt.orElseThrow(() -> 
					new NotFoundException("Payment not found: " + request.paymentId())))
				.get();
			
			validatePayment(payment, request.amount());
			
			payment.setStatus(PaymentStatus.PROCESSING);
			payment = resilientPaymentOps.savePayment(payment).get();
			
			PaymentTransaction transaction = saveTransactionResilient(
				payment.getId(), TransactionType.CAPTURE, request.amount(), payment.getCurrency(),
				"CAP-" + UUID.randomUUID(), idempotencyKey, request.description()
			);
			
			payment.setStatus(PaymentStatus.COMPLETED);
			payment.setCompletedAt(Instant.now());
			resilientPaymentOps.savePayment(payment).get();
			
			saveIdempotencyRecord(idempotencyKey, payment.getId(), transaction.getId());
			
			// Publish event to outbox and Kafka
			publishTransactionEvent(transaction);
			
			log.info("Successfully processed payment: {}", request.paymentId());
			return toPaymentTransactionResponse(transaction);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			log.error("Interrupted while processing payment: {}", request.paymentId(), ex);
			throw new PaymentServiceException("Payment processing interrupted", ex);
		} catch (ExecutionException ex) {
			log.error("Failed to process payment: {}", request.paymentId(), ex);
			throw new PaymentServiceException("Payment processing failed", ex.getCause());
		} catch (Exception ex) {
			log.error("Failed to process payment: {}", request.paymentId(), ex);
			throw ex;
		} finally {
			lock.unlock();
		}
	}

	@Transactional
	@CacheEvict(value = "payment-status", key = "#request.paymentId()")
	@Timed(value = "payment.refund", description = "Time taken to refund payment")
	public PaymentTransactionResponse refundPayment(RefundPaymentRequest request, String idempotencyKey) {
		log.info("Refunding payment: {} amount: {}", request.paymentId(), request.refundAmount());
		
		rateLimiter.acquirePermission();
		
		if (idempotencyKey != null && !idempotencyKey.isBlank()) {
			PaymentTransactionResponse existing = getExistingIdempotentTransaction(idempotencyKey);
			if (existing != null) {
				log.debug("Idempotent transaction found for key: {}. Returning cached response.", idempotencyKey);
				return existing;
			}
		}

		ReentrantLock lock = paymentLockManager.getLock(request.paymentId());
		lock.lock();
		try {
			Payment payment = resilientPaymentOps.findPaymentWithLock(request.paymentId())
				.thenApply(opt -> opt.orElseThrow(() -> 
					new NotFoundException("Payment not found: " + request.paymentId())))
				.get();
			
			if (payment.getStatus() != PaymentStatus.COMPLETED) {
				throw new BadRequestException("Can only refund completed payments. Current status: " + payment.getStatus());
			}
			
			if (request.refundAmount().compareTo(payment.getAmount()) > 0) {
				throw new BadRequestException("Refund amount exceeds payment amount");
			}
			
			payment.setStatus(PaymentStatus.REFUNDED);
			payment = resilientPaymentOps.savePayment(payment).get();
			
			PaymentTransaction transaction = saveTransactionResilient(
				payment.getId(), TransactionType.REFUND, request.refundAmount(), payment.getCurrency(),
				"REF-" + UUID.randomUUID(), idempotencyKey, request.reason()
			);
			
			saveIdempotencyRecord(idempotencyKey, payment.getId(), transaction.getId());
			
			// Publish event to outbox and Kafka
			publishTransactionEvent(transaction);
			
			log.info("Successfully refunded payment: {}", request.paymentId());
			return toPaymentTransactionResponse(transaction);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			log.error("Interrupted while refunding payment: {}", request.paymentId(), ex);
			throw new PaymentServiceException("Payment refund interrupted", ex);
		} catch (ExecutionException ex) {
			log.error("Failed to refund payment: {}", request.paymentId(), ex);
			throw new PaymentServiceException("Payment refund failed", ex.getCause());
		} catch (Exception ex) {
			log.error("Failed to refund payment: {}", request.paymentId(), ex);
			throw ex;
		} finally {
			lock.unlock();
		}
	}

	// ============== Helper Methods ==============

	private void validatePayment(Payment payment, BigDecimal requestAmount) {
		if (payment.getStatus() != PaymentStatus.PENDING) {
			throw new BadRequestException("Payment is not in pending state. Status: " + payment.getStatus());
		}
		if (payment.getAmount().compareTo(requestAmount) != 0) {
			throw new BadRequestException("Request amount does not match payment amount");
		}
	}

	private PaymentTransaction saveTransactionResilient(
		UUID paymentId,
		TransactionType type,
		BigDecimal amount,
		String currency,
		String referenceId,
		String idempotencyKey,
		String description
	) {
		try {
			PaymentTransaction tx = new PaymentTransaction();
			tx.setPaymentId(paymentId);
			tx.setType(type);
			tx.setAmount(amount);
			tx.setCurrency(currency.toUpperCase());
			tx.setReferenceId(referenceId);
			tx.setIdempotencyKey(idempotencyKey);
			tx.setDescription(description);
			
			return resilientTransactionOps.saveTransaction(tx).get();
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			log.error("Interrupted while saving transaction", ex);
			throw new PaymentServiceException("Transaction save interrupted", ex);
		} catch (ExecutionException ex) {
			log.error("Failed to save transaction", ex);
			throw new PaymentServiceException("Failed to save transaction", ex.getCause());
		}
	}

	private void saveIdempotencyRecord(String idempotencyKey, UUID paymentId, UUID transactionId) {
		if (idempotencyKey == null || idempotencyKey.isBlank()) {
			return;
		}
		try {
			IdempotencyRecord record = new IdempotencyRecord();
			record.setIdempotencyKey(idempotencyKey);
			record.setPaymentId(paymentId);
			record.setTransactionId(transactionId.toString());
			idempotencyRecordRepository.save(record);
			log.debug("Idempotency record saved for key: {}", idempotencyKey);
		} catch (Exception ex) {
			log.warn("Failed to save idempotency record for key: {}", idempotencyKey, ex);
		}
	}

	/**
	 * Publish payment transaction event using Transactional Outbox Pattern.
	 * Event is first saved to outbox table, then published to Kafka by the relay task.
	 * This guarantees event delivery even if Kafka is temporarily unavailable.
	 */
	private void publishTransactionEvent(PaymentTransaction tx) {
		try {
			PaymentTransactionEvent event = new PaymentTransactionEvent(
				tx.getId(),
				tx.getPaymentId(),
				tx.getType().name(),
				tx.getAmount(),
				tx.getCurrency(),
				tx.getReferenceId(),
				Instant.now()
			);
			
			// Step 1: Save to outbox table (within same transaction as payment update)
			paymentOutboxService.enqueueTransactionEvent(event);
			log.debug("Payment transaction event enqueued to outbox for transaction: {}", tx.getId());
			
			// Step 2: Try to publish immediately (best effort)
			// If this fails, the outbox relay task will retry it
			paymentEventPublisher.publishTransaction(event);
		} catch (Exception ex) {
			log.warn("Failed to publish payment transaction event immediately. Will retry via outbox relay. " +
				"Transaction: {}", tx.getId(), ex);
			// Event is in outbox, it will be picked up by the relay task
		}
	}

	private PaymentTransactionResponse getExistingIdempotentTransaction(String idempotencyKey) {
		if (idempotencyKey == null || idempotencyKey.isBlank()) {
			return null;
		}
		
		try {
			var fromDb = idempotencyRecordRepository.findById(idempotencyKey)
				.flatMap(record -> transactionRepository.findById(UUID.fromString(record.getTransactionId())))
				.map(this::toPaymentTransactionResponse);
			
			if (fromDb.isPresent()) {
				log.debug("Found idempotent transaction in database for key: {}", idempotencyKey);
				return fromDb.get();
			}
		} catch (Exception ex) {
			log.warn("Error checking for existing idempotent transaction", ex);
		}
		
		return null;
	}

	private PaymentResponse toPaymentResponse(Payment payment) {
		return new PaymentResponse(
			payment.getId(),
			payment.getMerchantId(),
			payment.getCustomerId(),
			payment.getAmount(),
			payment.getCurrency(),
			payment.getStatus().name(),
			payment.getReferenceId(),
			payment.getDescription()
		);
	}

	private PaymentTransactionResponse toPaymentTransactionResponse(PaymentTransaction tx) {
		return new PaymentTransactionResponse(
			tx.getId(),
			tx.getPaymentId(),
			tx.getType().name(),
			tx.getAmount(),
			tx.getCurrency(),
			tx.getReferenceId(),
			tx.getDescription()
		);
	}
}