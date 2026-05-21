package com.wallet.smart.pay.hub.sph.resilience;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.micrometer.core.annotation.Timed;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import com.wallet.smart.pay.hub.sph.entity.IdempotencyRecord;
import com.wallet.smart.pay.hub.sph.entity.WalletTransaction;
import com.wallet.smart.pay.hub.sph.exception.WalletServiceException;
import com.wallet.smart.pay.hub.sph.outbox.OutboxStatus;
import com.wallet.smart.pay.hub.sph.outbox.WalletOutboxEvent;
import com.wallet.smart.pay.hub.sph.repository.IdempotencyRecordRepository;
import com.wallet.smart.pay.hub.sph.repository.WalletOutboxEventRepository;
import com.wallet.smart.pay.hub.sph.repository.WalletTransactionRepository;

/**
 * Resilient wrapper for transaction-related database operations.
 * Includes circuit breaker, retry, and time limiter patterns.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ResilientTransactionOperations {

    private final WalletTransactionRepository transactionRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final WalletOutboxEventRepository outboxEventRepository;

    @CircuitBreaker(name = "walletDB", fallbackMethod = "saveTransactionFallback")
    @Retry(name = "walletDB")
    @TimeLimiter(name = "walletDB")
    @Timed(value = "wallet.transaction.save", description = "Time taken to save transaction")
    public CompletableFuture<WalletTransaction> saveTransaction(WalletTransaction transaction) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Saving transaction: {} for wallet: {}", transaction.getId(), transaction.getWalletId());
                return transactionRepository.save(transaction);
            } catch (DataAccessException ex) {
                log.error("Database error while saving transaction", ex);
                throw new WalletServiceException("Failed to save transaction", ex);
            }
        });
    }

    @CircuitBreaker(name = "walletDB", fallbackMethod = "findTransactionByIdFallback")
    @Retry(name = "walletDB")
    @TimeLimiter(name = "walletDB")
    @Timed(value = "wallet.transaction.find", description = "Time taken to find transaction")
    public CompletableFuture<Optional<WalletTransaction>> findTransactionById(UUID transactionId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return transactionRepository.findById(transactionId);
            } catch (DataAccessException ex) {
                log.error("Database error while finding transaction: {}", transactionId, ex);
                throw new WalletServiceException("Failed to find transaction: " + transactionId, ex);
            }
        });
    }

    @CircuitBreaker(name = "walletDB", fallbackMethod = "saveIdempotencyRecordFallback")
    @Retry(name = "walletDB")
    @TimeLimiter(name = "walletDB")
    @Timed(value = "wallet.idempotency.save", description = "Time taken to save idempotency record")
    public CompletableFuture<IdempotencyRecord> saveIdempotencyRecord(IdempotencyRecord record) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Saving idempotency record for key: {}", record.getIdempotencyKey());
                return idempotencyRecordRepository.save(record);
            } catch (DataAccessException ex) {
                log.error("Database error while saving idempotency record", ex);
                throw new WalletServiceException("Failed to save idempotency record", ex);
            }
        });
    }

    @CircuitBreaker(name = "walletDB", fallbackMethod = "findIdempotencyRecordFallback")
    @Retry(name = "walletDB")
    @TimeLimiter(name = "walletDB")
    @Timed(value = "wallet.idempotency.find", description = "Time taken to find idempotency record")
    public CompletableFuture<Optional<IdempotencyRecord>> findIdempotencyRecord(String idempotencyKey) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return idempotencyRecordRepository.findById(idempotencyKey);
            } catch (DataAccessException ex) {
                log.error("Database error while finding idempotency record", ex);
                throw new WalletServiceException("Failed to find idempotency record", ex);
            }
        });
    }

    @CircuitBreaker(name = "walletDB", fallbackMethod = "findPendingOutboxEventsFallback")
    @Retry(name = "walletDB")
    @TimeLimiter(name = "walletDB")
    @Timed(value = "wallet.outbox.find.pending", description = "Time taken to find pending outbox events")
    public CompletableFuture<List<WalletOutboxEvent>> findPendingOutboxEvents() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
            } catch (DataAccessException ex) {
                log.error("Database error while finding pending outbox events", ex);
                throw new WalletServiceException("Failed to find pending outbox events", ex);
            }
        });
    }

    @CircuitBreaker(name = "walletDB", fallbackMethod = "saveOutboxEventFallback")
    @Retry(name = "walletDB")
    @TimeLimiter(name = "walletDB")
    @Timed(value = "wallet.outbox.save", description = "Time taken to save outbox event")
    public CompletableFuture<WalletOutboxEvent> saveOutboxEvent(WalletOutboxEvent event) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return outboxEventRepository.save(event);
            } catch (DataAccessException ex) {
                log.error("Database error while saving outbox event", ex);
                throw new WalletServiceException("Failed to save outbox event", ex);
            }
        });
    }

    // Fallback methods
    public CompletableFuture<WalletTransaction> saveTransactionFallback(WalletTransaction transaction, Throwable ex) {
        log.warn("Circuit breaker triggered for saveTransaction", ex);
        return CompletableFuture.failedFuture(
            new WalletServiceException("Transaction save service temporarily unavailable", ex)
        );
    }

    public CompletableFuture<Optional<WalletTransaction>> findTransactionByIdFallback(UUID transactionId, Throwable ex) {
        log.warn("Circuit breaker triggered for findTransactionById", ex);
        return CompletableFuture.failedFuture(
            new WalletServiceException("Transaction lookup temporarily unavailable", ex)
        );
    }

    public CompletableFuture<IdempotencyRecord> saveIdempotencyRecordFallback(IdempotencyRecord record, Throwable ex) {
        log.warn("Circuit breaker triggered for saveIdempotencyRecord", ex);
        return CompletableFuture.failedFuture(
            new WalletServiceException("Idempotency record service temporarily unavailable", ex)
        );
    }

    public CompletableFuture<Optional<IdempotencyRecord>> findIdempotencyRecordFallback(String key, Throwable ex) {
        log.warn("Circuit breaker triggered for findIdempotencyRecord", ex);
        return CompletableFuture.failedFuture(
            new WalletServiceException("Idempotency lookup temporarily unavailable", ex)
        );
    }

    public CompletableFuture<List<WalletOutboxEvent>> findPendingOutboxEventsFallback(Throwable ex) {
        log.warn("Circuit breaker triggered for findPendingOutboxEvents", ex);
        return CompletableFuture.failedFuture(
            new WalletServiceException("Outbox events service temporarily unavailable", ex)
        );
    }

    public CompletableFuture<WalletOutboxEvent> saveOutboxEventFallback(WalletOutboxEvent event, Throwable ex) {
        log.warn("Circuit breaker triggered for saveOutboxEvent", ex);
        return CompletableFuture.failedFuture(
            new WalletServiceException("Outbox event save service temporarily unavailable", ex)
        );
    }
}

