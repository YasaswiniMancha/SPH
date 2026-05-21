package com.wallet.smart.pay.hub.sph.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import com.wallet.smart.pay.hub.sph.dto.AmountRequest;
import com.wallet.smart.pay.hub.sph.dto.CreateWalletRequest;
import com.wallet.smart.pay.hub.sph.dto.TransactionResponse;
import com.wallet.smart.pay.hub.sph.dto.TransferRequest;
import com.wallet.smart.pay.hub.sph.dto.WalletResponse;
import com.wallet.smart.pay.hub.sph.concurrency.WalletLockManager;
import com.wallet.smart.pay.hub.sph.entity.IdempotencyRecord;
import com.wallet.smart.pay.hub.sph.entity.TransactionType;
import com.wallet.smart.pay.hub.sph.entity.Wallet;
import com.wallet.smart.pay.hub.sph.entity.WalletStatus;
import com.wallet.smart.pay.hub.sph.entity.WalletTransaction;
import com.wallet.smart.pay.hub.sph.event.WalletTransactionEvent;
import com.wallet.smart.pay.hub.sph.exception.BadRequestException;
import com.wallet.smart.pay.hub.sph.exception.NotFoundException;
import com.wallet.smart.pay.hub.sph.exception.WalletServiceException;
import com.wallet.smart.pay.hub.sph.outbox.WalletOutboxService;
import com.wallet.smart.pay.hub.sph.repository.IdempotencyRecordRepository;
import com.wallet.smart.pay.hub.sph.repository.WalletRepository;
import com.wallet.smart.pay.hub.sph.repository.WalletTransactionRepository;
import com.wallet.smart.pay.hub.sph.resilience.ResilientTransactionOperations;
import com.wallet.smart.pay.hub.sph.resilience.ResilientWalletOperations;
import com.wallet.smart.pay.hub.sph.resilience.TransactionRateLimiter;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ExecutionException;

/**
 * Enhanced Wallet Service with comprehensive resilience patterns:
 * - Circuit breakers for database operations
 * - Retry mechanisms for transient failures
 * - Rate limiting for traffic control
 * - Optimized transaction handling
 * - Caching strategy for performance
 * Supports 10k+ transactions per day with high-traffic resilience
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final IdempotencyCacheService idempotencyCacheService;
    private final WalletOutboxService walletOutboxService;
    private final WalletLockManager walletLockManager;
    private final ResilientWalletOperations resilientWalletOps;
    private final ResilientTransactionOperations resilientTransactionOps;
    private final TransactionRateLimiter rateLimiter;

    @Transactional
    @Timed(value = "wallet.creation", description = "Time taken to create a wallet")
    public WalletResponse createWallet(CreateWalletRequest request) {
        log.info("Creating wallet for customer: {}", request.customerId());
        
        rateLimiter.acquirePermission();
        
        try {
            // Check if wallet already exists
            var existing = resilientWalletOps.findByCustomerId(request.customerId()).get();
            if (existing.isPresent()) {
                throw new BadRequestException("Wallet already exists for customerId: " + request.customerId());
            }

            // Create new wallet
            Wallet wallet = new Wallet();
            wallet.setCustomerId(request.customerId());
            wallet.setCurrency(request.currency().toUpperCase());
            wallet.setBalance(request.openingBalance());
            wallet.setStatus(WalletStatus.ACTIVE);
            
            wallet = resilientWalletOps.saveWallet(wallet).get();
            log.info("Wallet created successfully for customer: {}", request.customerId());
            return toWalletResponse(wallet);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while creating wallet for customer: {}", request.customerId(), ex);
            throw new WalletServiceException("Wallet creation interrupted", ex);
        } catch (ExecutionException ex) {
            log.error("Failed to create wallet for customer: {}", request.customerId(), ex);
            throw new BadRequestException("Failed to create wallet: " + ex.getCause().getMessage());
        }
    }

    @Cacheable(value = "wallet-balance", key = "#walletId")
    @Transactional(readOnly = true)
    @Timed(value = "wallet.balance.fetch", description = "Time taken to fetch wallet balance")
    public WalletResponse getBalance(UUID walletId) {
        log.debug("Fetching balance for wallet: {}", walletId);
        
        try {
            // Use resilient wallet operations with circuit breaker, retry, and timeout
            Wallet wallet = resilientWalletOps.findWalletById(walletId)
                .thenApply(opt -> opt.orElseThrow(() -> 
                    new NotFoundException("Wallet not found: " + walletId)))
                .get();
            
            return toWalletResponse(wallet);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while fetching wallet: {}", walletId, ex);
            throw new WalletServiceException("Wallet fetch interrupted", ex);
        } catch (ExecutionException ex) {
            log.error("Failed to fetch wallet: {}", walletId, ex);
            throw new WalletServiceException("Failed to fetch wallet: " + walletId, ex.getCause());
        }
    }

    @Transactional
    @CacheEvict(value = "wallet-balance", key = "#walletId")
    @Timed(value = "wallet.credit", description = "Time taken to credit wallet")
    public TransactionResponse credit(UUID walletId, AmountRequest request, String idempotencyKey) {
        log.info("Crediting wallet: {} with amount: {}", walletId, request.amount());
        
        // Rate limiting
        rateLimiter.acquirePermission();
        
        // Idempotency check
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            TransactionResponse existing = getExistingIdempotentTransaction(idempotencyKey);
            if (existing != null) {
                log.debug("Idempotent transaction found for key: {}. Returning cached response.", idempotencyKey);
                return existing;
            }
        }

        ReentrantLock lock = walletLockManager.getLock(walletId);
        lock.lock();
        try {
            // Fetch wallet with lock using resilient operations
            Wallet wallet = resilientWalletOps.findWalletWithLock(walletId)
                .thenApply(opt -> opt.orElseThrow(() -> 
                    new NotFoundException("Wallet not found: " + walletId)))
                .get();
            
            validateWallet(wallet, request.currency());

            // Perform credit operation
            wallet.setBalance(wallet.getBalance().add(request.amount()));
            wallet = resilientWalletOps.saveWallet(wallet).get();
            
            // Save transaction using resilient operations
            WalletTransaction transaction = saveTransactionResilient(
                wallet.getId(), TransactionType.CREDIT, request.amount(), wallet.getCurrency(),
                "CREDIT-" + UUID.randomUUID(), idempotencyKey, request.description()
            );
            
            // Save idempotency record
            saveIdempotencyRecord(idempotencyKey, wallet.getId(), transaction.getId());
            
            // Publish event via outbox
            publishTransactionEvent(transaction);
            
            log.info("Successfully credited wallet: {} with amount: {}", walletId, request.amount());
            return toTransactionResponse(transaction);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while crediting wallet: {}", walletId, ex);
            throw new WalletServiceException("Credit operation interrupted", ex);
        } catch (ExecutionException ex) {
            log.error("Failed to credit wallet: {}", walletId, ex);
            throw new WalletServiceException("Credit operation failed", ex.getCause());
        } catch (Exception ex) {
            log.error("Failed to credit wallet: {}", walletId, ex);
            throw ex;
        } finally {
            lock.unlock();
        }
    }

    @Transactional
    @CacheEvict(value = "wallet-balance", key = "#walletId")
    @Timed(value = "wallet.debit", description = "Time taken to debit wallet")
    public TransactionResponse debit(UUID walletId, AmountRequest request, String idempotencyKey) {
        log.info("Debiting wallet: {} with amount: {}", walletId, request.amount());
        
        // Rate limiting
        rateLimiter.acquirePermission();
        
        // Idempotency check
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            TransactionResponse existing = getExistingIdempotentTransaction(idempotencyKey);
            if (existing != null) {
                log.debug("Idempotent transaction found for key: {}. Returning cached response.", idempotencyKey);
                return existing;
            }
        }

        ReentrantLock lock = walletLockManager.getLock(walletId);
        lock.lock();
        try {
            // Fetch wallet with lock using resilient operations
            Wallet wallet = resilientWalletOps.findWalletWithLock(walletId)
                .thenApply(opt -> opt.orElseThrow(() -> 
                    new NotFoundException("Wallet not found: " + walletId)))
                .get();
            
            validateWallet(wallet, request.currency());

            // Check sufficient balance
            if (wallet.getBalance().compareTo(request.amount()) < 0) {
                throw new BadRequestException("Insufficient wallet balance. Available: " + wallet.getBalance() + 
                    ", Requested: " + request.amount());
            }

            // Perform debit operation
            wallet.setBalance(wallet.getBalance().subtract(request.amount()));
            wallet = resilientWalletOps.saveWallet(wallet).get();
            
            // Save transaction using resilient operations
            WalletTransaction transaction = saveTransactionResilient(
                wallet.getId(), TransactionType.DEBIT, request.amount(), wallet.getCurrency(),
                "DEBIT-" + UUID.randomUUID(), idempotencyKey, request.description()
            );
            
            // Save idempotency record
            saveIdempotencyRecord(idempotencyKey, wallet.getId(), transaction.getId());
            
            // Publish event via outbox
            publishTransactionEvent(transaction);
            
            log.info("Successfully debited wallet: {} with amount: {}", walletId, request.amount());
            return toTransactionResponse(transaction);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while debiting wallet: {}", walletId, ex);
            throw new WalletServiceException("Debit operation interrupted", ex);
        } catch (ExecutionException ex) {
            log.error("Failed to debit wallet: {}", walletId, ex);
            throw new WalletServiceException("Debit operation failed", ex.getCause());
        } catch (Exception ex) {
            log.error("Failed to debit wallet: {}", walletId, ex);
            throw ex;
        } finally {
            lock.unlock();
        }
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "wallet-balance", key = "#request.fromWalletId()"),
        @CacheEvict(value = "wallet-balance", key = "#request.toWalletId()")
    })
    @Timed(value = "wallet.transfer", description = "Time taken to transfer between wallets")
    public TransactionResponse transfer(TransferRequest request, String idempotencyKey) {
        log.info("Transferring {} from wallet {} to wallet {}", request.amount(), 
            request.fromWalletId(), request.toWalletId());
        
        // Rate limiting
        rateLimiter.acquirePermission();
        
        // Validate transfer request
        if (request.fromWalletId().equals(request.toWalletId())) {
            throw new BadRequestException("Transfer source and destination cannot be same");
        }

        // Idempotency check
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            TransactionResponse existing = getExistingIdempotentTransaction(idempotencyKey);
            if (existing != null) {
                log.debug("Idempotent transaction found for key: {}. Returning cached response.", idempotencyKey);
                return existing;
            }
        }

        // Order locks to prevent deadlock
        UUID firstLockId = request.fromWalletId().compareTo(request.toWalletId()) <= 0
            ? request.fromWalletId() : request.toWalletId();
        UUID secondLockId = request.fromWalletId().compareTo(request.toWalletId()) <= 0
            ? request.toWalletId() : request.fromWalletId();

        ReentrantLock firstLock = walletLockManager.getLock(firstLockId);
        ReentrantLock secondLock = walletLockManager.getLock(secondLockId);
        
        firstLock.lock();
        secondLock.lock();
        try {
            // Fetch both wallets using resilient operations
            Wallet first = resilientWalletOps.findWalletWithLock(firstLockId)
                .thenApply(opt -> opt.orElseThrow(() -> 
                    new NotFoundException("Wallet not found: " + firstLockId)))
                .get();
            
            Wallet second = resilientWalletOps.findWalletWithLock(secondLockId)
                .thenApply(opt -> opt.orElseThrow(() -> 
                    new NotFoundException("Wallet not found: " + secondLockId)))
                .get();

            Wallet from = first.getId().equals(request.fromWalletId()) ? first : second;
            Wallet to = first.getId().equals(request.toWalletId()) ? first : second;

            // Validate wallets
            validateWallet(from, from.getCurrency());
            validateWallet(to, to.getCurrency());
            
            if (!from.getCurrency().equalsIgnoreCase(to.getCurrency())) {
                throw new BadRequestException("Wallet currency mismatch: " + from.getCurrency() + " vs " + to.getCurrency());
            }
            
            if (from.getBalance().compareTo(request.amount()) < 0) {
                throw new BadRequestException("Insufficient wallet balance. Available: " + from.getBalance() + 
                    ", Requested: " + request.amount());
            }

            // Execute transfer
            from.setBalance(from.getBalance().subtract(request.amount()));
            to.setBalance(to.getBalance().add(request.amount()));
            
            // Save both wallets using resilient operations
            resilientWalletOps.saveWallet(from).get();
            resilientWalletOps.saveWallet(to).get();

            // Create transfer transactions
            String transferRef = "TRF-" + UUID.randomUUID();
            WalletTransaction debitTxn = saveTransactionResilient(from.getId(), TransactionType.TRANSFER_OUT, request.amount(),
                from.getCurrency(), transferRef, idempotencyKey, request.description());
            WalletTransaction creditTxn = saveTransactionResilient(to.getId(), TransactionType.TRANSFER_IN, request.amount(),
                to.getCurrency(), transferRef, idempotencyKey, request.description());
            
            // Save idempotency record
            saveIdempotencyRecord(idempotencyKey, to.getId(), creditTxn.getId());
            
            // Publish events
            publishTransactionEvent(debitTxn);
            publishTransactionEvent(creditTxn);
            
            log.info("Successfully transferred {} from wallet {} to wallet {}", 
                request.amount(), request.fromWalletId(), request.toWalletId());
            return toTransactionResponse(creditTxn);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("Interrupted during transfer from {} to {}", request.fromWalletId(), request.toWalletId(), ex);
            throw new WalletServiceException("Transfer interrupted", ex);
        } catch (ExecutionException ex) {
            log.error("Failed to transfer: {} to {}", request.fromWalletId(), request.toWalletId(), ex);
            throw new WalletServiceException("Transfer failed", ex.getCause());
        } catch (Exception ex) {
            log.error("Failed to transfer: {} to {}", request.fromWalletId(), request.toWalletId(), ex);
            throw ex;
        } finally {
            secondLock.unlock();
            firstLock.unlock();
        }
    }

    // ============== Helper Methods ==============

    private void validateWallet(Wallet wallet, String requestCurrency) {
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new BadRequestException("Wallet is not active. Status: " + wallet.getStatus());
        }
        if (!wallet.getCurrency().equalsIgnoreCase(requestCurrency)) {
            throw new BadRequestException("Request currency (" + requestCurrency + 
                ") does not match wallet currency (" + wallet.getCurrency() + ")");
        }
    }

    private WalletTransaction saveTransactionResilient(
        UUID walletId,
        TransactionType type,
        BigDecimal amount,
        String currency,
        String referenceId,
        String idempotencyKey,
        String description
    ) {
        try {
            WalletTransaction tx = new WalletTransaction();
            tx.setWalletId(walletId);
            tx.setType(type);
            tx.setAmount(amount);
            tx.setCurrency(currency.toUpperCase());
            tx.setReferenceId(referenceId);
            tx.setIdempotencyKey(idempotencyKey);
            tx.setDescription(description);
            
            // Use resilient transaction operations with circuit breaker, retry, and timeout
            return resilientTransactionOps.saveTransaction(tx).get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while saving transaction", ex);
            throw new WalletServiceException("Transaction save interrupted", ex);
        } catch (ExecutionException ex) {
            log.error("Failed to save transaction", ex);
            throw new WalletServiceException("Failed to save transaction", ex.getCause());
        }
    }

    private WalletTransaction saveTransaction(
        UUID walletId,
        TransactionType type,
        BigDecimal amount,
        String currency,
        String referenceId,
        String idempotencyKey,
        String description
    ) {
        WalletTransaction tx = new WalletTransaction();
        tx.setWalletId(walletId);
        tx.setType(type);
        tx.setAmount(amount);
        tx.setCurrency(currency.toUpperCase());
        tx.setReferenceId(referenceId);
        tx.setIdempotencyKey(idempotencyKey);
        tx.setDescription(description);
        return transactionRepository.save(tx);
    }

    private void saveIdempotencyRecord(String idempotencyKey, UUID walletId, UUID transactionId) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }
        try {
            IdempotencyRecord record = new IdempotencyRecord();
            record.setIdempotencyKey(idempotencyKey);
            record.setWalletId(walletId);
            record.setTransactionId(transactionId.toString());
            idempotencyRecordRepository.save(record);
            idempotencyCacheService.putTransactionId(idempotencyKey, transactionId);
            log.debug("Idempotency record saved for key: {}", idempotencyKey);
        } catch (Exception ex) {
            log.warn("Failed to save idempotency record for key: {}", idempotencyKey, ex);
            // Don't fail the transaction if idempotency record save fails
        }
    }

    private TransactionResponse getExistingIdempotentTransaction(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        
        try {
            // Try cache first
            var fromCache = idempotencyCacheService.getTransactionId(idempotencyKey)
                .flatMap(transactionRepository::findById)
                .map(this::toTransactionResponse);
            
            if (fromCache.isPresent()) {
                log.debug("Found idempotent transaction in cache for key: {}", idempotencyKey);
                return fromCache.get();
            }
            
            // Try database
            var fromDb = idempotencyRecordRepository.findById(idempotencyKey)
                .flatMap(record -> transactionRepository.findById(UUID.fromString(record.getTransactionId())))
                .map(this::toTransactionResponse);
            
            if (fromDb.isPresent()) {
                log.debug("Found idempotent transaction in database for key: {}", idempotencyKey);
                // Update cache
                fromDb.ifPresent(resp -> 
                    idempotencyCacheService.putTransactionId(idempotencyKey, resp.transactionId())
                );
                return fromDb.get();
            }
        } catch (Exception ex) {
            log.warn("Error checking for existing idempotent transaction", ex);
        }
        
        return null;
    }

    private void publishTransactionEvent(WalletTransaction tx) {
        try {
            walletOutboxService.enqueueTransactionEvent(new WalletTransactionEvent(
                tx.getId(),
                tx.getWalletId(),
                tx.getType().name(),
                tx.getAmount(),
                tx.getCurrency(),
                tx.getReferenceId(),
                Instant.now()
            ));
            log.debug("Transaction event enqueued for transaction: {}", tx.getId());
        } catch (Exception ex) {
            log.error("Failed to enqueue transaction event", ex);
            // Don't fail the transaction if event publishing fails
        }
    }

    private WalletResponse toWalletResponse(Wallet wallet) {
        return new WalletResponse(
            wallet.getId(),
            wallet.getCustomerId(),
            wallet.getCurrency(),
            wallet.getBalance(),
            wallet.getStatus().name()
        );
    }

    private TransactionResponse toTransactionResponse(WalletTransaction tx) {
        return new TransactionResponse(
            tx.getId(),
            tx.getWalletId(),
            tx.getType().name(),
            tx.getAmount(),
            tx.getCurrency(),
            tx.getReferenceId(),
            tx.getDescription()
        );
    }
}
