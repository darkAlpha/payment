# Payment System - Deposit Product

> Core Banking Payment System built with **Apache Dubbo 3.3 + Spring Boot 3.1** — DDD, Hexagonal Architecture, API-First, TDD

## 🏗️ Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                     payment-connector (REST API Gateway)          │
│         JWT Auth │ RBAC │ Rate Limiting │ Request Logging         │
└───────────────────────────────┬──────────────────────────────────┘
                                │ Dubbo Triple Protocol
┌───────────────────────────────▼──────────────────────────────────┐
│                        payment-deposit (Service)                   │
│  ┌─────────────┐  ┌──────────────┐  ┌────────────────────────┐  │
│  │  Interfaces  │  │  Application  │  │   Infrastructure       │  │
│  │  (Dubbo)     │──│  (Use Cases)  │──│  (Adapters/Persistence)│  │
│  └─────────────┘  └──────┬───────┘  └────────────────────────┘  │
│                           │                                       │
│                    ┌──────▼───────┐                               │
│                    │    Domain     │   (Pure business rules)       │
│                    └──────────────┘                               │
└──────────────────────────────────────────────────────────────────┘
         │                    │                     │
    ┌────▼────┐        ┌─────▼─────┐        ┌─────▼──────┐
    │ Ledger  │        │Notification│        │  Customer   │
    │ Service │        │  Service   │        │  Service    │
    └─────────┘        └───────────┘        └────────────┘
```

## 📦 Module Structure

| Module | Description |
|--------|-------------|
| `payment-api` | Dubbo service interfaces & DTOs (API-First contracts) |
| `payment-core` | Cross-cutting: exceptions, i18n, idempotency, cache, security, resilience |
| `payment-deposit` | Deposit business logic (DDD bounded context) |
| `payment-connector` | REST API Gateway with security |
| `payment-service` | Legacy payment service |
| `payment-gateway` | Payment gateway integration |

## 🔧 Operations

| Operation | Endpoint | Description |
|-----------|----------|-------------|
| Create Deposit | `POST /api/v1/deposit` | Open new deposit account |
| Close Deposit | `POST /api/v1/deposit/close` | Close deposit, return funds |
| Deposit → Card | `POST /api/v1/deposit/transfer/deposit-to-card` | Transfer from deposit to card |
| Card → Deposit | `POST /api/v1/deposit/transfer/card-to-deposit` | Transfer from card to deposit |
| Deposit → Deposit | `POST /api/v1/deposit/transfer/deposit-to-deposit` | Inter-deposit transfer |
| Transaction History | `GET /api/v1/deposit/history/{customerId}` | View deposit history |
| EOD Accrual | Scheduled (23:59 daily) | Calculate daily interest |

## 🔐 Security & RBAC

**Authentication:** JWT Bearer Token

**Roles:**
| Role | Permissions |
|------|-------------|
| `TELLER` | Create, Close, Transfer, View |
| `ADMIN` | All operations + admin endpoints |
| `SYSTEM` | Create, Transfer, EOD (service-to-service) |
| `AUDITOR` | View/History only |

**Usage:**
```bash
curl -X POST http://localhost:8080/api/v1/deposit \
  -H "Authorization: Bearer <jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{"customerId":"cust-001","accountId":"acc-001","amount":10000,"currency":"KZT"}'
```

## 🛡️ Cross-Cutting Concerns

### Idempotency
```java
@Idempotent(key = "#command.idempotencyKey()", ttlSeconds = 86400)
public Result execute(Command command) { ... }
```
- Redis-backed (production) or In-Memory (dev)
- Configurable TTL
- Automatic lock management

### Resilience (Retry + Circuit Breaker)
```java
@Resilient(name = "ledger-service", maxRetries = 3, retryDelayMs = 500)
public LedgerResult postEntry(...) { ... }
```
- Exponential backoff
- Business exceptions bypass retry
- Circuit breaker for external services

### i18n Error Messages
```
messages.properties      (English - default)
messages_ru.properties   (Russian)
messages_kk.properties   (Kazakh)
```

### Cache Abstraction
```yaml
payment.cache.provider: caffeine  # or: redis
```

### Audit Logging
Every state transition is recorded:
```
DEPOSIT_CREATED → CUSTOMER_VALIDATED → LEDGER_POSTED → DEPOSIT_COMPLETED
```

## 🚀 Quick Start

### Prerequisites
- Java 21+
- Docker & Docker Compose
- Maven 3.8+

### Run Infrastructure
```bash
docker-compose up -d  # MySQL, Zookeeper, Redis
```

### Build & Run
```bash
./mvnw clean install -DskipTests
./mvnw spring-boot:run -pl payment-deposit
./mvnw spring-boot:run -pl payment-connector
```

### Run Tests
```bash
./mvnw test -pl payment-deposit
```

## 📁 DDD Package Structure (payment-deposit)

```
src/main/java/org/tars/deposit/
├── domain/                          ← Pure domain (zero dependencies)
│   ├── model/
│   │   ├── DepositAccount.java      ← Aggregate Root
│   │   ├── DepositAccountStatus.java
│   │   ├── Transaction.java         ← Entity
│   │   ├── TransactionType.java
│   │   ├── Deposit.java             ← Legacy aggregate
│   │   ├── DepositStatus.java
│   │   └── Money.java               ← Value Object
│   ├── repository/                  ← Port interfaces
│   ├── event/                       ← Domain events
│   └── exception/                   ← Domain exceptions
├── application/                     ← Use cases & orchestration
│   ├── port/input/                  ← Use case interfaces (ISP)
│   │   ├── CreateDepositUseCase
│   │   ├── CloseDepositUseCase
│   │   ├── TransferUseCase
│   │   ├── AccrualUseCase
│   │   └── GetDepositHistoryUseCase
│   ├── port/output/                 ← External service ports (DIP)
│   │   ├── LedgerServicePort
│   │   ├── NotificationPort
│   │   ├── CustomerServicePort
│   │   └── AccountServicePort
│   ├── dto/                         ← Commands & Results
│   └── service/                     ← Use case implementations
├── infrastructure/                  ← Technical adapters
│   ├── persistence/                 ← JPA entities & repositories
│   ├── adapter/                     ← External service clients
│   ├── audit/                       ← Audit logging
│   └── scheduler/                   ← EOD scheduler
└── interfaces/                      ← Inbound adapters
    └── dubbo/                       ← Dubbo service implementation
```

## 👨‍💻 Developer Notes

### Adding a New Operation

1. **Define API** in `payment-api`:
   ```java
   // Add method to DepositService interface
   // Add Request/Response DTOs
   ```

2. **Create Use Case port** in `application/port/input/`:
   ```java
   public interface NewOperationUseCase {
       Result execute(Command command);
   }
   ```

3. **Implement Use Case** in `application/service/`:
   ```java
   @Service
   @Transactional
   public class NewOperationService implements NewOperationUseCase {
       // Inject ports, orchestrate domain
   }
   ```

4. **Wire Dubbo adapter** in `interfaces/dubbo/DepositServiceImpl`

5. **Add REST endpoint** in `payment-connector`

6. **Write tests first** (TDD):
   - Unit test domain model
   - Unit test service with mocked ports
   - Integration test (optional)

### Configuration Properties

```yaml
# Idempotency
payment.idempotency.store: redis  # memory | redis

# Cache
payment.cache.provider: caffeine  # caffeine | redis

# Security
payment.security.jwt.secret: <256-bit-secret>
payment.security.jwt.expiration-ms: 3600000

# EOD
payment.eod.cron: "0 59 23 * * *"

# External Services
customer.service.url: http://localhost:8081
account.service.url: http://localhost:8082
```

## 🗺️ Roadmap & Extension Strategy

### Phase 1 (Current) ✅
- [x] Deposit CRUD operations
- [x] Transfer operations (deposit↔card, deposit↔deposit)
- [x] EOD interest accrual
- [x] Security (JWT + RBAC)
- [x] Idempotency
- [x] Audit trail
- [x] i18n error messages
- [x] Cache abstraction
- [x] Resilience/retry

### Phase 2 (Next)
- [ ] Event sourcing for deposit state changes
- [ ] Saga pattern for distributed transactions
- [ ] gRPC/Triple streaming for real-time notifications
- [ ] OpenAPI/Swagger documentation
- [ ] Prometheus + Grafana monitoring
- [ ] Distributed tracing (Jaeger/Zipkin)

### Phase 3 (Future)
- [ ] Multi-tenancy support
- [ ] Deposit product catalog (flexible terms/rates)
- [ ] Auto-renewal on maturity
- [ ] Penalty calculation for early withdrawal
- [ ] Bulk operations API
- [ ] Admin dashboard (React/Vue frontend)
- [ ] CDC (Change Data Capture) for analytics

### How to Extend for New Products

```
1. Create new module: payment-{product}/
2. Define Dubbo API in payment-api
3. Implement DDD bounded context
4. Register in parent pom.xml
5. Add REST endpoints in payment-connector
6. Configure security roles
```

## 🧪 Testing Strategy

| Layer | Test Type | Tools |
|-------|-----------|-------|
| Domain | Unit | JUnit 5 |
| Application | Unit (mocked ports) | JUnit 5 + Mockito |
| Infrastructure | Integration | SpringBootTest + H2/Testcontainers |
| API | Contract | Dubbo Mock |
| E2E | Acceptance | REST Assured + Docker |

## 📝 License

Internal - TARS Payment Platform

