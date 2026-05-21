# SPH-WalletService - Resilience & High-Traffic Implementation

## Overview
This document describes the comprehensive resilience patterns and high-traffic optimization implemented in the SPH-WalletService to support 10k+ transactions per day with robust error handling and circuit breaker mechanisms.

## Architecture Components

### 1. **Resilience4j Integration**

#### Circuit Breaker Pattern
**Purpose**: Prevent cascading failures when downstream services are unavailable

**Configuration** (in `application.properties`):
```properties
resilience4j.circuitbreaker.instances.walletDB.sliding-window-size=100
resilience4j.circuitbreaker.instances.walletDB.failure-rate-threshold=50.0
resilience4j.circuitbreaker.instances.walletDB.wait-duration-in-open-state=60000
```

**Implementation**:
- **CLOSED State**: Normal operation, requests pass through
- **OPEN State**: Failures exceed threshold (50%), fast-fail new requests
- **HALF_OPEN State**: Test limited requests to determine recovery

**Usage in Code**:
```java
@CircuitBreaker(name = "walletDB", fallbackMethod = "findByIdFallback")
public CompletableFuture<Optional<Wallet>> findWalletById(UUID walletId)
```

---

### 2. **Retry Mechanism**

**Purpose**: Handle transient failures (network hiccups, temporary database issues)

**Configuration**:
```properties
resilience4j.retry.instances.walletDB.max-attempts=3
resilience4j.retry.instances.walletDB.wait-duration=500
resilience4j.retry.instances.walletDB.retry-exceptions=java.sql.SQLException,org.springframework.dao.DataAccessException
```

**Implementation**:
- Retries up to 3 times with 500ms wait between attempts
- Only retries on specific exceptions (SQL, DataAccess)
- Exponential backoff strategy via configuration

**Usage**:
```java
@Retry(name = "walletDB")
public CompletableFuture<Optional<Wallet>> findWithLockFallback(UUID walletId)
```

---

### 3. **Timeout Pattern**

**Purpose**: Prevent requests from hanging indefinitely

**Configuration**:
```properties
resilience4j.timelimiter.instances.walletDB.timeout-duration=5s
resilience4j.timelimiter.instances.walletDB.cancel-running-future=true
```

**Implementation**:
- 5-second timeout for all database operations
- Automatically cancels hanging futures
- Better resource management

**Usage**:
```java
@TimeLimiter(name = "walletDB")
public CompletableFuture<Optional<Wallet>> findWalletById(UUID walletId)
```

---

### 4. **Bulkhead Pattern**

**Purpose**: Isolate resources to prevent thread pool exhaustion

**Configuration**:
```properties
resilience4j.bulkhead.instances.walletDB.max-concurrent-calls=50
resilience4j.bulkhead.instances.walletDB.max-wait-duration=0
```

**Implementation**:
- Maximum 50 concurrent database calls
- Protects thread pool from being consumed by one operation type
- Provides predictable resource allocation

---

### 5. **Rate Limiting**

**Purpose**: Manage traffic and prevent overload

**Configuration**:
```properties
resilience4j.ratelimiter.instances.walletTransactions.limit-refresh-period=1m
resilience4j.ratelimiter.instances.walletTransactions.limit-for-period=1000
```

**Implementation**:
- 1000 requests per minute (≈16.67 TPS)
- Handles 10k transactions/day easily (0.12 TPS average)
- Burst handling up to 16.67 TPS

**Usage in WalletService**:
```java
public TransactionResponse credit(UUID walletId, AmountRequest request, String idempotencyKey) {
    rateLimiter.acquirePermission();  // Rate limit check
    // ... rest of operation
}
```

**Metrics**:
Access rate limit metrics via actuator endpoint:
```
GET /actuator/metrics/resilience4j.ratelimiter.waits
```

---

### 6. **Metrics & Monitoring**

**Available Endpoints**:
```
GET /actuator/health
GET /actuator/metrics
GET /actuator/metrics/resilience4j.circuitbreaker.*
GET /actuator/metrics/resilience4j.retry.*
GET /actuator/metrics/http.server.requests
```

**Custom Metrics**:
```java
@Timed(value = "wallet.creation", description = "Time taken to create a wallet")
public WalletResponse createWallet(CreateWalletRequest request)
```

---

## Resilient Components

### 1. **ResilientWalletOperations**
Handles wallet-level database operations with comprehensive error handling

```java
@CircuitBreaker(name = "walletDB")
@Retry(name = "walletDB")
@TimeLimiter(name = "walletDB")
@Timed(value = "wallet.operation.find")
public CompletableFuture<Optional<Wallet>> findWalletById(UUID walletId)
```

**Methods**:
- `findWalletById()` - Fetch wallet by ID
- `findWalletWithLock()` - Fetch with pessimistic lock
- `saveWallet()` - Save wallet changes
- `findByCustomerId()` - Find wallet by customer

---

### 2. **ResilientTransactionOperations**
Handles transaction and outbox-related operations

**Methods**:
- `saveTransaction()` - Save wallet transaction
- `findTransactionById()` - Fetch transaction
- `saveIdempotencyRecord()` - Save idempotency key
- `findPendingOutboxEvents()` - Fetch pending events for relay
- `saveOutboxEvent()` - Save outbox event

---

### 3. **ResilientKafkaPublisher**
Publishes events to Kafka with fallback to outbox pattern

```java
@CircuitBreaker(name = "kafkaPublisher")
@Retry(name = "kafkaPublisher")
public void publishTransactionEvent(String topic, WalletTransactionEvent event)
```

**Circuit Breaker Config**:
```properties
resilience4j.circuitbreaker.instances.kafkaPublisher.failure-rate-threshold=50.0
resilience4j.circuitbreaker.instances.kafkaPublisher.wait-duration-in-open-state=30000
```

**Fallback Strategy**:
- When circuit breaks, events remain in outbox
- Outbox relay picks them up and retries via scheduled task
- Ensures no message loss

---

### 4. **TransactionRateLimiter**
Manages transaction rate limits for traffic control

```java
public void acquirePermission() {
    // Throws exception if rate limit exceeded
    // Returns immediately if permission granted
}

public boolean tryAcquirePermission() {
    // Non-blocking check, returns true/false
}
```

---

## Performance Optimizations for 10k+ Transactions/Day

### 1. **Connection Pooling**
```properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=10000
```

### 2. **Redis Caching**
```properties
spring.data.redis.lettuce.pool.max-active=20
spring.data.redis.lettuce.pool.min-idle=5
wallet.cache.balance.ttl-minutes=2
```

### 3. **Thread Pool Configuration**
```java
// Wallet event executor
executor.setCorePoolSize(10);
executor.setMaxPoolSize(20);
executor.setQueueCapacity(1000);

// Transaction executor
executor.setCorePoolSize(15);
executor.setMaxPoolSize(30);
executor.setQueueCapacity(2000);
```

### 4. **Kafka Optimization**
```properties
spring.kafka.producer.batch-size=16384
spring.kafka.producer.linger-ms=10
spring.kafka.producer.compression-type=snappy
```

### 5. **Tomcat Thread Configuration**
```properties
server.tomcat.threads.max=200
server.tomcat.threads.min-spare=50
server.tomcat.max-connections=10000
```

---

## High-Traffic Support Strategy

### Traffic Capacity Calculation

**Metric**: 10,000 transactions per day
- **Average TPS**: 0.116 (very low)
- **Peak TPS** (estimated): 16.67 (1000 per minute)
- **Rate Limiter Threshold**: 1000 req/min

### Handling Spikes

1. **Thread Pools**: Scale to 30-50 threads for burst handling
2. **Queue Capacity**: 1000-2000 requests buffering
3. **Rate Limiter**: Graceful degradation with clear error messages
4. **Circuit Breaker**: Fast-fail to prevent cascading failures

### Resource Distribution

```
Total Threads: ~250 (Tomcat: 200, Event Executor: 20, Tx Executor: 30)
DB Connections: 20 (HikariCP)
Redis Connections: 20
Concurrent Bulkhead Calls: 50
Queue Capacity: >3000
```

---

## Error Handling & Fallbacks

### Database Failures
```
Attempt 1 → Fail → Wait 500ms
Attempt 2 → Fail → Wait 500ms
Attempt 3 → Fail → Circuit opens for 60s
After 60s → Half-open state → Limited test requests
```

### Kafka Failures
```
Event publishing failed → Remains in outbox
Outbox relay picks up → Retries every 1.5 seconds
Circuit opens for 30s → Outbox handles queue
```

### Rate Limit Exceeded
```
HTTP 429 Too Many Requests
Wait-Duration: Calculated backoff
Message: "Transaction rate limit exceeded. Please retry after X ms"
```

---

## Monitoring & Alerting

### Key Metrics to Monitor

1. **Circuit Breaker Health**
   ```
   resilience4j.circuitbreaker.state (0=CLOSED, 1=OPEN, 2=HALF_OPEN)
   resilience4j.circuitbreaker.failure.calls
   ```

2. **Request Latency**
   ```
   http.server.requests{quantile="0.95"}
   http.server.requests{quantile="0.99"}
   ```

3. **Rate Limiter**
   ```
   resilience4j.ratelimiter.available.permissions
   resilience4j.ratelimiter.waiting.threads
   ```

4. **Retry Metrics**
   ```
   resilience4j.retry.calls{kind="retry"}
   resilience4j.retry.calls{kind="successful_without_retry"}
   ```

### Alert Thresholds

- Circuit Breaker OPEN → Alert
- P99 Latency > 1s → Warning
- P95 Latency > 500ms → Info
- Failed Outbox Events > 10 → Alert
- Rate Limit Rejections > 5/min → Warning

---

## Configuration Tuning Guide

### For Higher Load (50k+ transactions/day)

```properties
# Increase thread pools
server.tomcat.threads.max=400
executor.setCorePoolSize(20)
executor.setMaxPoolSize(50)

# Increase rate limit
resilience4j.ratelimiter.instances.walletTransactions.limit-for-period=2000

# Increase connection pool
spring.datasource.hikari.maximum-pool-size=30
spring.data.redis.lettuce.pool.max-active=30
```

### For Lower Latency

```properties
# Reduce timeouts
resilience4j.timelimiter.instances.walletDB.timeout-duration=2s

# Increase circuit breaker sensitivity
resilience4j.circuitbreaker.instances.walletDB.failure-rate-threshold=30.0

# Increase retry wait
resilience4j.retry.instances.walletDB.wait-duration=100
```

---

## Testing Resilience

### Circuit Breaker Test
```bash
# Simulate database failure for 60 seconds
# Monitor /actuator/health to see CIRCUIT_OPEN status

# After 60s, observe HALF_OPEN state
# After successful test requests, observe CLOSED state
```

### Rate Limit Test
```bash
# Send >1000 requests per minute
# Observe 429 responses after threshold

curl -X POST http://localhost:8083/api/v1/wallets/credit \
  -H "Content-Type: application/json" \
  --data '{"amount": 100, "currency": "USD"}'
```

### Timeout Test
```bash
# Simulate slow database (>5s responses)
# Observe TimeoutException in logs
```

---

## Best Practices

1. **Always use Idempotency Keys** for critical operations
2. **Monitor circuit breaker states** in real-time
3. **Test failover** scenarios regularly
4. **Configure appropriate timeouts** for your infrastructure
5. **Use rate limiter** for traffic spike protection
6. **Keep outbox events purged** (archive old events)
7. **Scale predictively** based on capacity planning
8. **Use async operations** for non-critical paths

---

## References

- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [Circuit Breaker Pattern](https://martinfowler.com/bliki/CircuitBreaker.html)
- [Bulkhead Pattern](https://docs.microsoft.com/en-us/azure/architecture/patterns/bulkhead)
- [Transactional Outbox Pattern](https://microservices.io/patterns/data/transactional-outbox.html)

