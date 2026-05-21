package com.wallet.smart.pay.hub.sph.resilience;

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

import com.wallet.smart.pay.hub.sph.entity.Wallet;
import com.wallet.smart.pay.hub.sph.exception.WalletServiceException;
import com.wallet.smart.pay.hub.sph.repository.WalletRepository;

/**
 * Resilient wrapper for wallet database operations with Resilience4j patterns:
 * - Circuit Breaker: Prevents cascading failures
 * - Retry: Handles transient failures
 * - Time Limiter: Prevents hanging requests
 * - Metrics: Tracks operation success/failure rates
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ResilientWalletOperations {

    private final WalletRepository walletRepository;
    private static final long MAX_TIMEOUT_SECONDS = 5;
    private static final int MAX_RETRIES = 3;

    @CircuitBreaker(name = "walletDB", fallbackMethod = "findByIdFallback")
    @Retry(name = "walletDB")
    @TimeLimiter(name = "walletDB")
    @Timed(value = "wallet.operation.find", description = "Time taken to find wallet by ID")
    public CompletableFuture<Optional<Wallet>> findWalletById(UUID walletId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Fetching wallet with ID: {}", walletId);
                return walletRepository.findById(walletId);
            } catch (DataAccessException ex) {
                log.error("Database error while fetching wallet: {}", walletId, ex);
                throw new WalletServiceException("Failed to fetch wallet: " + walletId, ex);
            }
        });
    }
    //basically we want to use pessimistic locking here to ensure that we can safely update the wallet without running into concurrent modification issues. This is especially important in a high-concurrency environment where multiple transactions might be trying to access and modify the same wallet record at the same time. By using a pessimistic lock, we can ensure that once a transaction has acquired the lock on a wallet record, other transactions will be blocked from accessing it until the lock is released, thus preventing data inconsistencies and ensuring the integrity of our wallet data.
    @CircuitBreaker(name = "walletDB", fallbackMethod = "findWithLockFallback")
    @Retry(name = "walletDB")
    @TimeLimiter(name = "walletDB")
    @Timed(value = "wallet.operation.find.lock", description = "Time taken to find wallet with lock")
    public CompletableFuture<Optional<Wallet>> findWalletWithLock(UUID walletId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Fetching wallet with pessimistic lock: {}", walletId);
                return walletRepository.findWithLockById(walletId);
            }  catch(OptimisticLockingFailureException ex){
                log.warn("Optimistic lock conflict while fetching wallet: {}", walletId);
                throw new WalletServiceException("Wallet was modified concurrently: " + walletId);
            } catch ( DataAccessException ex) {
                log.error("Database error while fetching wallet with lock: {}", walletId, ex);
                throw new WalletServiceException("Failed to fetch wallet with lock: " + walletId, ex);
            }
        });
    }

    @CircuitBreaker(name = "walletDB", fallbackMethod = "saveWalletFallback")
    @Retry(name = "walletDB")
    @TimeLimiter(name = "walletDB")
    @Timed(value = "wallet.operation.save", description = "Time taken to save wallet")
    public CompletableFuture<Wallet> saveWallet(Wallet wallet) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Saving wallet: {}", wallet.getId());
                return walletRepository.save(wallet);
            } catch (OptimisticLockingFailureException ex) {
                log.warn("Optimistic lock conflict while saving wallet: {}", wallet.getId());
                throw new WalletServiceException("Wallet was modified concurrently", ex);
            } catch (DataAccessException ex) {
                log.error("Database error while saving wallet", ex);
                throw new WalletServiceException("Failed to save wallet", ex);
            }
        });
    }

    @CircuitBreaker(name = "walletDB", fallbackMethod = "findByCustomerIdFallback")
    @Retry(name = "walletDB")
    @TimeLimiter(name = "walletDB")
    @Timed(value = "wallet.operation.find.customer", description = "Time taken to find wallet by customer ID")
    public CompletableFuture<Optional<Wallet>> findByCustomerId(String customerId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Finding wallet for customer: {}", customerId);
                return walletRepository.findByCustomerId(customerId);
            } catch (DataAccessException ex) {
                log.error("Database error while finding wallet for customer: {}", customerId, ex);
                throw new WalletServiceException("Failed to find wallet for customer: " + customerId, ex);
            }
        });
    }

    // Fallback methods for circuit breaker
    public CompletableFuture<Optional<Wallet>> findByIdFallback(UUID walletId, Throwable ex) {
        log.warn("Circuit breaker triggered for findWalletById. Returning empty.", ex);
        return CompletableFuture.failedFuture(
            new WalletServiceException("Wallet service temporarily unavailable", ex)
        );
    }

    public CompletableFuture<Optional<Wallet>> findWithLockFallback(UUID walletId, Throwable ex) {
        log.warn("Circuit breaker triggered for findWalletWithLock. Returning empty.", ex);
        return CompletableFuture.failedFuture(
            new WalletServiceException("Wallet lock service temporarily unavailable", ex)
        );
    }

    public CompletableFuture<Wallet> saveWalletFallback(Wallet wallet, Throwable ex) {
        log.warn("Circuit breaker triggered for saveWallet", ex);
        return CompletableFuture.failedFuture(
            new WalletServiceException("Unable to persist wallet changes", ex)
        );
    }

    public CompletableFuture<Optional<Wallet>> findByCustomerIdFallback(String customerId, Throwable ex) {
        log.warn("Circuit breaker triggered for findByCustomerId", ex);
        return CompletableFuture.failedFuture(
            new WalletServiceException("Customer wallet lookup temporarily unavailable", ex)
        );
    }
}

