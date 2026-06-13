# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [0.2.0] - 2026-06-13

### Added
- **Deposit Operations**
  - Create deposit account with initial amount, interest rate, term
  - Close deposit with balance settlement
  - Deposit to card transfer
  - Card to deposit transfer
  - Deposit to deposit inter-account transfer
  
- **EOD (End of Day) Processing**
  - Daily interest accrual for all active deposit accounts
  - Configurable cron schedule (`payment.eod.cron`)
  - Automatic ledger posting for accrued interest
  
- **Security & RBAC**
  - JWT Bearer token authentication
  - Role-based access control (TELLER, ADMIN, SYSTEM, AUDITOR)
  - Method-level security with `@PreAuthorize`
  - Stateless session management
  
- **Idempotency**
  - `@Idempotent` annotation with SpEL key expression
  - Redis-backed store (production)
  - In-memory store (development)
  - Configurable TTL
  
- **Resilience**
  - `@Resilient` annotation with retry + exponential backoff
  - Circuit breaker pattern for external services
  - Business exceptions bypass retry logic
  
- **Internationalization (i18n)**
  - Error messages in English, Russian, Kazakh
  - `BusinessException` with error codes mapped to message bundles
  - Global exception handler with locale-aware message resolution
  
- **Cache Abstraction**
  - Caffeine (local, default)
  - Redis (distributed)
  - Switch via `payment.cache.provider` property
  
- **Audit Logging**
  - `deposit_audit_log` table tracking all state transitions
  - REQUIRES_NEW propagation (survives transaction rollbacks)
  - Records: action, status before/after, details, timestamp
  
- **Transaction History**
  - `GET /api/v1/deposit/history/{customerId}`
  - Full transaction log per deposit account
  - Ordered by most recent first
  
- **Notification Service**
  - Async notification port
  - Template-based messaging (SMS, EMAIL, PUSH)
  - Non-blocking with retry
  
- **Ledger Integration**
  - Double-entry bookkeeping for transfers
  - DEBIT/CREDIT entries per operation
  - Reference tracking

- **Infrastructure**
  - `payment-core` module for shared cross-cutting concerns
  - `payment-api` expanded with all deposit operation DTOs
  - `payment-deposit` full DDD implementation
  - `payment-connector` secured REST gateway

### Technical Details
- Java 21
- Spring Boot 3.1.2
- Apache Dubbo 3.3.0
- Spring Data JPA (MySQL)
- Spring Security 6
- Resilience4j
- JWT (jjwt 0.12.3)
- Caffeine Cache
- Redis (optional)
- H2 (tests)
- Lombok
- JUnit 5 + Mockito

## [0.1.0] - 2026-06-12

### Added
- Initial project setup
- `payment-api` module with Dubbo service definitions
- `payment-service` module with basic debit-to-credit transfer
- `payment-connector` module with REST gateway
- Docker Compose configuration (MySQL, Zookeeper)
- Basic Dubbo Triple protocol communication

---

## Migration Notes

### From 0.1.0 to 0.2.0
1. Run database migration to create new tables:
   - `deposit_accounts`
   - `deposit_transactions`
   - `deposit_audit_log`
   - `deposits` (legacy)
2. Add Redis configuration if using distributed idempotency/cache
3. Configure JWT secret in production: `payment.security.jwt.secret`
4. Review RBAC roles and assign to users

