# SPH-WalletService - Resilience Refactoring Guide

## 📋 Overview

This document explains the refactoring done to integrate **Resilience4j patterns** into the `WalletService` class. Previously, the resilient components were created but not utilized. This refactoring ensures **all database operations now go through resilient wrappers** with proper fault tolerance.

---

## 🔴 **THE PROBLEM (Before Refactoring)**

### Unused Resilience Components
```java
// Fields were declared but NEVER USED:
private final ResilientWalletOperations resilientWalletOps;      // ❌ UNUSED
private final ResilientTransactionOperations resilientTransactionOps; // ❌ UNUSED
```

### Direct Repository Calls (No Resilience)
```java
// Direct repository calls BYPASSED all resilience patterns:
Wallet wallet = walletRepository.findById(walletId)  // ❌ No circuit breaker
    .orElseThrow(...);

walletRepository.save(wallet);  // ❌ No retry mechanism

transactionRepository.save(transaction);  // ❌ No timeout protection
```

### Missing Benefits
- ❌ **No Circuit Breaker** - Could cascade failures
- ❌ **No Retry Logic** - Transient failures fail immediately
- ❌ **No Timeout Protection** - Hanging requests could block threads
- ❌ **No Metrics** - No observability into failures
- ❌ **No Automatic Recovery** - Service not self-healing

---

## 🟢 **THE SOLUTION (After Refactoring)**

### 1. **All Wallet Operations Now Use Resilient Wrappers**

#### Before:
```java
Wallet wallet = walletRepository.findById(walletId)
    .orElseThrow(() -> new NotFoundException(...));
```

#### After:
```java
// ✅ Wrapped with circuit breaker, retry, and timeout
Wallet wallet = resilientWalletOps.findWalletWithLock(walletId)
    .thenApply(opt -> opt.orElseThrow(() -> new NotFoundException(...)))
    .get();  // Sync call to maintain transaction context
```

---

### 2. **All Transaction Operations Now Use Resilient Wrappers**

#### Before:
```java
WalletTransaction transaction = saveTransaction(...);
  // Direct repository call - no resilience
```

#### After:
```java
// ✅ New resilient method uses ResilientTransactionOperations
WalletTransaction transaction = saveTransactionResilient(...);
  // Wrapped with circuit breaker, retry, and timeout
```

---

### 3. **Key Methods Refactored**

| Method | Changes |
|--------|---------|
| `getBalance()` | Now uses `resilientWalletOps.findWalletById()` |
| `createWallet()` | Now uses `resilientWalletOps.findByCustomerId()` + `saveWallet()` |
| `credit()` | Now uses `resilientWalletOps.findWalletWithLock()` + resilient save |
| `debit()` | Now uses `resilientWalletOps.findWalletWithLock()` + resilient save |
| `transfer()` | Now uses both wallet and transaction resilient ops |

---

## 🛡️ **Resilience Pattern Details**

### Circuit Breaker (walletDB)
```properties
# Application.properties configuration:
resilience4j.circuitbreaker.instances.walletDB.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.walletDB.wait-duration-in-open-state=60000ms
```

**What it does:**
- Monitors database failures
- After 50% failures, opens circuit (fails fast)
- Recovers automatically after 60 seconds
- Prevents cascading failures

### Retry Mechanism
```properties
resilience4j.retry.instances.walletDB.max-attempts=3
resilience4j.retry.instances.walletDB.wait-duration=500ms
```

**What it does:**
- Automatically retries on transient failures
- Waits 500ms between attempts (exponential backoff)
- Max 3 attempts before failing
- Handles temporary DB issues

### Time Limiter
```properties
resilience4j.timelimiter.instances.walletDB.timeout-duration=5000ms
```

**What it does:**
- Prevents operations from hanging
- Times out after 5 seconds
- Prevents thread pool exhaustion
- Critical for high-traffic scenarios

---

## 📊 **Operations Flow**

### Resilient Wallet Operations
```
Request
  ↓
Rate Limiter (allow/deny)
  ↓
Resilient Operation (with async CompletableFuture)
  ├─ Circuit Breaker Guard
  ├─ Retry Logic (3 attempts)
  ├─ Time Limiter (5 sec timeout)
  ├─ Database Call
  └─ Metrics Recording
  ↓
Response
```

### Resilient Transaction Operations
```
Transaction Save Request
  ↓
Resilient Operation
  ├─ Circuit Breaker Check
  ├─ Retry if transient failure
  ├─ Timeout guard
  ├─ Database Insert
  └─ Metrics
  ↓
Idempotency Record
  ↓
Response
```

---

## 🔄 **AsyncToSync Pattern**

Since `WalletService` methods need to return synchronously (for `@Transactional` context), we use:

```java
// Async CompletableFuture wrapped with resilience
try {
    Wallet wallet = resilientWalletOps.findWalletById(walletId)
        .thenApply(opt -> opt.orElseThrow(...))
        .get();  // ← Blocks until complete (safe in transaction context)
} catch (InterruptedException ex) {
    Thread.currentThread().interrupt();
    throw new WalletServiceException(...);
} catch (ExecutionException ex) {
    // Extract actual exception
    throw new WalletServiceException(..., ex.getCause());
}
```

**Why this approach:**
- ✅ Maintains transactional semantics
- ✅ Preserves pessimistic locking behavior
- ✅ Enables fallback methods on circuit breaker
- ✅ Provides complete timeout protection

---

## 🎯 **Benefits After Refactoring**

### 1. **Failure Detection & Recovery**
```
DB Failure
  ↓
Circuit Breaker Activates (after 3+ failures)
  ↓
Fallback Method Triggered
  ↓
Client Gets Proper Error (not hanging)
  ↓
Auto-Recovery After 60 Seconds
```

### 2. **Transient Failure Handling**
```
Network Timeout
  ↓
Automatic Retry (500ms wait)
  ↓
Success on Retry
  ↓
Operation Completes Normally
```

### 3. **Timeout Protection**
```
Long DB Query (> 5 seconds)
  ↓
Time Limiter Triggers
  ↓
Thread Released, Error Returned
  ↓
Thread Pool Not Exhausted
```

### 4. **High-Traffic Support**
```
10,000 requests/day = 0.116 TPS average
Peak capacity: 16.67 TPS (1000 req/min rate limit)

With resilience:
- Bulkhead: 50 concurrent DB operations max
- Rate Limiter: Queues excess requests
- Timeout: No hanging threads
- Result: Graceful degradation under load
```

---

## 📈 **Metrics Available**

After refactoring, metrics are now collected:

```
# Circuit Breaker Metrics
io.github.resilience4j.circuitbreaker.walletDB.calls.successful
io.github.resilience4j.circuitbreaker.walletDB.calls.failed
io.github.resilience4j.circuitbreaker.walletDB.state

# Retry Metrics
io.github.resilience4j.retry.walletDB.calls.successful
io.github.resilience4j.retry.walletDB.calls.retried
io.github.resilience4j.retry.walletDB.calls.failed

# Method Timers
wallet.operation.find (in milliseconds)
wallet.operation.save
wallet.transaction.save
wallet.transfer
```

Access via: `curl http://localhost:8083/actuator/prometheus`

---

## 🚀 **Testing the Resilience**

### Test 1: Simulate Database Failure
```bash
# Kill database temporarily
docker stop postgresql

# Try wallet operation - should fail fast (not hang)
curl -X GET http://localhost:8083/api/v1/wallets/{walletId}/balance

# Expected: Circuit breaker opens after 3 failures
# Then: 503 Service Unavailable (not timeout)
```

### Test 2: Transient Failure Recovery
```bash
# Cause intermittent failures
# Retry logic will handle them automatically
# Most requests succeed despite temporary issues
```

### Test 3: Load Testing
```bash
# Generate 100+ concurrent requests
ab -c 100 -n 1000 http://localhost:8083/api/v1/wallets/

# Expected:
# - Rate limiter queues excess requests
# - No thread pool exhaustion
# - Graceful degradation
# - All within timeout limits
```

---

## 🔧 **Configuration Tuning**

### For Higher Load (20k+ transactions/day):
```properties
# Increase rate limit
resilience4j.ratelimiter.instances.wallet.limit-refresh-period=1m
resilience4j.ratelimiter.instances.wallet.limit-for-period=2000

# Increase bulkhead
resilience4j.bulkhead.instances.walletDB.max-concurrent-calls=100

# Increase thread pool
management.endpoints.web.exposure.include=prometheus,health
```

### For Stricter Reliability (financial transactions):
```properties
# Stricter circuit breaker
resilience4j.circuitbreaker.instances.walletDB.failure-rate-threshold=30
resilience4j.circuitbreaker.instances.walletDB.slow-call-rate-threshold=50

# More retries
resilience4j.retry.instances.walletDB.max-attempts=5
resilience4j.retry.instances.walletDB.wait-duration=1000ms
```

---

## 📝 **Summary of Changes**

### Files Modified:
1. **WalletService.java** - All methods now use resilient operations
   - ✅ `createWallet()` - Uses `resilientWalletOps.findByCustomerId()`, `saveWallet()`
   - ✅ `getBalance()` - Uses `resilientWalletOps.findWalletById()`
   - ✅ `credit()` - Uses `resilientWalletOps.findWalletWithLock()`, `saveWallet()`
   - ✅ `debit()` - Uses `resilientWalletOps.findWalletWithLock()`, `saveWallet()`
   - ✅ `transfer()` - Uses both wallet and transaction resilient ops
   - ✅ Added `saveTransactionResilient()` helper

### Imports Added:
- `java.util.concurrent.CompletableFuture`
- `java.util.concurrent.ExecutionException`
- `com.wallet.smart.pay.hub.sph.exception.WalletServiceException`

### Exception Handling:
- ✅ Proper `InterruptedException` handling with thread interrupt restoration
- ✅ `ExecutionException` unwrapping to get actual cause
- ✅ Clear error messages for debugging

---

## ✅ **Validation Checklist**

After deployment, verify:

- [ ] All wallet operations complete successfully
- [ ] Metrics appear in `/actuator/prometheus`
- [ ] Circuit breaker status visible in `/actuator/health`
- [ ] Rate limiting prevents > 1000 req/min
- [ ] No hanging requests (all complete within 5 seconds)
- [ ] Retries help transient failures succeed
- [ ] High-traffic load doesn't exhaust thread pools
- [ ] Error messages are informative

---

## 🎓 **Learning Resources**

- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [Circuit Breaker Pattern](https://martinfowler.com/bliki/CircuitBreaker.html)
- [Java CompletableFuture Guide](https://www.baeldung.com/java-completablefuture)

---

## 🤝 **Support**

For issues, check:
1. Application metrics: `/actuator/prometheus`
2. Health status: `/actuator/health/circuitBreakers`
3. Logs for circuit breaker events
4. Performance metrics for capacity planning


