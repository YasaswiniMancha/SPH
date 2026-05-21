# SPH-WalletService - Implementation Guide

## Service Overview

The SPH-WalletService is a production-grade wallet management service built with Spring Boot 4.0.6 and Spring Data JPA. It implements comprehensive resilience patterns using Resilience4j to support 10,000+ transactions per day under high-traffic conditions.

## Key Features

### ✅ Transaction Management
- **Credit Operations**: Add funds to wallet
- **Debit Operations**: Remove funds from wallet
- **Transfers**: Move funds between wallets
- **Atomic Transactions**: Pessimistic locking prevents race conditions
- **Deadlock Prevention**: Ordered locking for transfers

### ✅ Resilience & Fault Tolerance
- **Circuit Breaker**: Prevents cascading failures (Resilience4j)
- **Retry Mechanism**: Handles transient failures automatically
- **Time Limiter**: Prevents request hangs (5 second timeout)
- **Bulkhead Pattern**: Resource isolation (max 50 concurrent DB calls)
- **Rate Limiting**: Traffic control (1000 requests/minute)

### ✅ Data Consistency
- **Idempotency**: Duplicate request detection via Idempotency-Key header
- **Cache-friendly**: Balance caching with 2-minute TTL
- **Transactional Outbox**: Event publishing guarantee via Kafka

### ✅ High-Traffic Support
- **Thread Pools**: 30+ concurrent transaction threads
- **Connection Pooling**: HikariCP with 20 connections
- **Kafka Batching**: Batch size 16KB, compression enabled
- **Redis Caching**: Distributed cache for balance queries

### ✅ Monitoring & Observability
- **Spring Actuator**: Health, metrics, info endpoints
- **Micrometer Integration**: Prometheus-compatible metrics
- **Timed Methods**: Automatic latency tracking
- **Debug Logging**: Comprehensive operation logging

## Project Structure

```
SPH-WalletService/
├── src/main/java/com/wallet/smart/pay/hub/sph/
│   ├── api/
│   │   ├── WalletController.java          # REST endpoints
│   │   └── dto/                            # Request/Response DTOs
│   ├── domain/
│   │   ├── Wallet.java                     # Wallet entity
│   │   ├── WalletTransaction.java          # Transaction entity
│   │   ├── IdempotencyRecord.java          # Idempotency tracking
│   │   ├── TransactionType.java            # CREDIT, DEBIT, TRANSFER_*
│   │   └── WalletStatus.java               # ACTIVE, BLOCKED
│   ├── service/
│   │   ├── WalletService.java              # Main business logic
│   │   └── IdempotencyCacheService.java    # Redis-based idempotency cache
│   ├── resilience/                         # NEW: Resilience components
│   │   ├── ResilientWalletOperations.java  # DB operation wrapper
│   │   ├── ResilientTransactionOperations.java  # Transaction wrapper
│   │   ├── ResilientKafkaPublisher.java    # Event publishing wrapper
│   │   └── TransactionRateLimiter.java     # Traffic control
│   ├── repository/                         # Data access layer
│   ├── event/                              # Event publishing
│   ├── outbox/                             # Transactional outbox pattern
│   ├── concurrency/                        # Lock management
│   ├── config/                             # Spring configuration
│   ├── exception/                          # Exception classes
│   └── SphWalletServiceApplication.java   # Entry point
└── src/main/resources/
    └── application.properties               # Configuration
```

## API Endpoints

### 1. Create Wallet
```http
POST /api/v1/wallets
Content-Type: application/json

{
  "customerId": "CUST-12345",
  "currency": "USD",
  "openingBalance": 1000.00
}

Response: 201 Created
{
  "walletId": "550e8400-e29b-41d4-a716-446655440000",
  "customerId": "CUST-12345",
  "currency": "USD",
  "balance": 1000.00,
  "status": "ACTIVE"
}
```

### 2. Get Wallet Balance
```http
GET /api/v1/wallets/{walletId}/balance

Response: 200 OK
{
  "walletId": "550e8400-e29b-41d4-a716-446655440000",
  "customerId": "CUST-12345",
  "currency": "USD",
  "balance": 1000.00,
  "status": "ACTIVE"
}
```

### 3. Credit Wallet
```http
POST /api/v1/wallets/{walletId}/credit
Content-Type: application/json
Idempotency-Key: CRED-2024-05-08-001

{
  "amount": 100.00,
  "currency": "USD",
  "description": "Online payment"
}

Response: 200 OK
{
  "transactionId": "txn-123456",
  "walletId": "550e8400-e29b-41d4-a716-446655440000",
  "type": "CREDIT",
  "amount": 100.00,
  "currency": "USD",
  "referenceId": "CREDIT-abc-def-123",
  "description": "Online payment"
}
```

### 4. Debit Wallet
```http
POST /api/v1/wallets/{walletId}/debit
Content-Type: application/json
Idempotency-Key: DEB-2024-05-08-001

{
  "amount": 50.00,
  "currency": "USD",
  "description": "Payment sent"
}
```

### 5. Transfer Between Wallets
```http
POST /api/v1/wallets/transfers
Content-Type: application/json
Idempotency-Key: TRF-2024-05-08-001

{
  "fromWalletId": "550e8400-e29b-41d4-a716-446655440000",
  "toWalletId": "660e8400-e29b-41d4-a716-446655440001",
  "amount": 200.00,
  "description": "Transfer to friend"
}
```

## Running the Service

### Prerequisites
- Java 21+
- PostgreSQL 12+
- Redis 6+
- Kafka 3+

### Database Setup
```sql
CREATE DATABASE smartpayhub_wallet;

-- Tables are auto-created by Hibernate (ddl-auto=update)
```

### Build & Package
```bash
cd SPH-WalletService
mvn clean package
```

### Run Service
```bash
# Development
mvn spring-boot:run

# Production
java -jar target/SPH-WalletService-0.0.1-SNAPSHOT.jar \
  --server.port=8083 \
  --DB_URL=jdbc:postgresql://prod-db:5432/smartpayhub_wallet \
  --REDIS_HOST=prod-redis \
  --KAFKA_BOOTSTRAP_SERVERS=prod-kafka:9092
```

### Environment Variables
```bash
# Database
DB_URL=jdbc:postgresql://localhost:5432/smartpayhub_wallet
DB_USERNAME=postgres
DB_PASSWORD=postgres

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
WALLET_EVENTS_TOPIC=wallet.transactions.v1

# Application
SERVER_PORT=8083
WALLET_BALANCE_CACHE_TTL_MINUTES=2
WALLET_OUTBOX_RELAY_DELAY_MS=1500
```

## Configuration

### application.properties

Key configurations for high-traffic support:

```properties
# Server Thread Pool
server.tomcat.threads.max=200
server.tomcat.threads.min-spare=50
server.tomcat.max-connections=10000

# Database Connection Pool
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5

# Resilience4j - Circuit Breaker
resilience4j.circuitbreaker.instances.walletDB.failure-rate-threshold=50.0
resilience4j.circuitbreaker.instances.walletDB.wait-duration-in-open-state=60000

# Resilience4j - Rate Limiting
resilience4j.ratelimiter.instances.walletTransactions.limit-for-period=1000

# Kafka
spring.kafka.producer.batch-size=16384
spring.kafka.producer.compression-type=snappy
```

See `RESILIENCE_ARCHITECTURE.md` for detailed configuration options.

## Monitoring & Health Checks

### Health Endpoint
```bash
curl http://localhost:8083/actuator/health

{
  "status": "UP",
  "components": {
    "circuitBreakers": {
      "status": "UP",
      "details": {
        "walletDB": "CLOSED",
        "kafkaPublisher": "CLOSED"
      }
    },
    "db": { "status": "UP" },
    "redis": { "status": "UP" },
    "kafka": { "status": "UP" }
  }
}
```

### Metrics Endpoint
```bash
curl http://localhost:8083/actuator/metrics

# Available metrics:
# - wallet.creation (timer)
# - wallet.balance.fetch (timer)
# - wallet.credit (timer)
# - wallet.debit (timer)
# - wallet.transfer (timer)
# - http.server.requests (histogram)
# - resilience4j.circuitbreaker.* (gauge)
# - resilience4j.ratelimiter.* (gauge)
```

### Prometheus Integration
```bash
curl http://localhost:8083/actuator/prometheus
```

## Error Handling

### HTTP Status Codes

| Code | Scenario |
|------|----------|
| 201 | Wallet created successfully |
| 200 | Transaction successful |
| 400 | Invalid request (validation, insufficient balance, etc.) |
| 404 | Wallet not found |
| 429 | Rate limit exceeded |
| 500 | Internal server error |
| 503 | Service temporarily unavailable (circuit open) |

### Error Response Format
```json
{
  "timestamp": "2024-05-08T10:15:30Z",
  "error": "BAD_REQUEST",
  "message": "Insufficient wallet balance. Available: 50.00, Requested: 100.00"
}
```

## Idempotency

All transaction endpoints support idempotency via the `Idempotency-Key` header:

```bash
curl -X POST http://localhost:8083/api/v1/wallets/550e8400/credit \
  -H "Idempotency-Key: CRED-2024-05-08-001" \
  -H "Content-Type: application/json" \
  -d '{"amount": 100.00, "currency": "USD"}'
```

**Benefits**:
- Safe to retry without creating duplicate transactions
- Returns same response for duplicate requests
- 24-hour cache retention

## Performance & Load Testing

### Expected Performance
- **Throughput**: 10,000+ transactions/day (~0.12 TPS average)
- **Peak Capacity**: 1000 requests/minute (16.67 TPS) with rate limiter
- **P99 Latency**: <500ms under normal load
- **P95 Latency**: <200ms under normal load

### Load Testing with Apache JMeter
```jmeter
# Create wallet test plan
- Thread Group: 50 threads, 10-minute ramp-up
- HTTP Request: POST /api/v1/wallets
- Response Assertion: Status 201

# Transaction test plan
- Thread Group: 100 threads, 5-minute ramp-up
- HTTP Request: POST /api/v1/wallets/{id}/credit
- Response Assertion: Status 200
```

### Stress Testing
```bash
# Using Apache Bench
ab -n 10000 -c 100 http://localhost:8083/api/v1/wallets

# Using wrk
wrk -t4 -c100 -d30s http://localhost:8083/api/v1/wallets
```

## Troubleshooting

### Circuit Breaker OPEN
**Symptom**: All requests return 503
**Cause**: Database failure rate exceeded 50%
**Solution**:
1. Check database connectivity
2. Monitor `/actuator/health`
3. Wait 60 seconds for auto-recovery
4. If persistent, scale database resources

### Rate Limit Exceeded
**Symptom**: HTTP 429 responses
**Cause**: Traffic exceeded 1000 req/min
**Solution**:
1. Implement request backoff in client
2. Increase rate limit if capacity allows
3. Scale horizontally with multiple instances

### High Latency
**Symptom**: P99 latency > 1 second
**Cause**: Database bottleneck or network issues
**Solution**:
1. Check database connection pool size
2. Monitor Redis connection pool
3. Increase server thread pool
4. Scale database resources

### Kafka Publishing Failures
**Symptom**: Outbox events accumulate
**Cause**: Kafka broker unavailable
**Solution**:
1. Check Kafka cluster health
2. Monitor circuit breaker status
3. Wait for auto-recovery
4. Manual outbox relay if needed

## Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn verify
```

### Resilience Tests
```bash
# Simulate database timeout
# Expected: Retry 3x, then circuit opens

# Send requests while circuit is open
# Expected: Fast failure with fallback response

# Monitor recovery after 60s
# Expected: Transition to HALF_OPEN, then CLOSED
```

## Deployment

### Docker
```dockerfile
FROM eclipse-temurin:21-jre-alpine
COPY target/SPH-WalletService-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Kubernetes
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: sph-wallet-service
spec:
  replicas: 3
  containers:
  - name: wallet
    image: wallet-service:latest
    ports:
    - containerPort: 8083
    livenessProbe:
      httpGet:
        path: /actuator/health
        port: 8083
      initialDelaySeconds: 30
      periodSeconds: 10
    resources:
      requests:
        memory: "512Mi"
        cpu: "500m"
      limits:
        memory: "1Gi"
        cpu: "1000m"
```

## Security Considerations

1. **Idempotency-Key**: Should be unique per transaction (UUID recommended)
2. **Input Validation**: All amounts and currencies validated
3. **Database Encryption**: Use SSL for DB connections in production
4. **Redis Security**: Enable Redis authentication in production
5. **API Authentication**: Implement OAuth2/JWT at API Gateway level
6. **Audit Logging**: All transactions logged for compliance

## License

Proprietary - SmartPayHub Payment System

## Support

For issues or questions, contact the FinTech Payment System team.

