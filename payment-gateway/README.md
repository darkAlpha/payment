# Payment Gateway — Spring Cloud Gateway

**Production-ready API Gateway** built on Spring Cloud Gateway (Netty + WebFlux).

## Why Spring Cloud Gateway?

| Custom Netty (before) | Spring Cloud Gateway (now) |
|---|---|
| 42 Java files, custom everything | **14 Java files** — framework handles the rest |
| Manual Netty server + pipeline | SCG auto-configures Netty |
| Custom proxy client | SCG built-in reverse proxy |
| Custom route registry | `spring.cloud.gateway.routes` in YAML |
| Custom filter chain | `GlobalFilter` + built-in `GatewayFilter` |
| Custom circuit breaker wiring | `spring-cloud-starter-circuitbreaker-reactor-resilience4j` |
| Custom retry logic | SCG `Retry` filter with backoff |
| Custom load balancer | SCG `lb://` URI scheme |
| Custom CORS handling | `spring.cloud.gateway.globalcors` |
| Custom health/metrics | Spring Boot Actuator |

## Architecture

```
                         ┌─────────────────────────────┐
                         │   Spring Cloud Gateway       │
                         │   (Netty + WebFlux)          │
Client ─── HTTPS ──────▶ ├─────────────────────────────┤
                         │ GlobalFilter Pipeline:        │
                         │  AccessLog → RateLimit →      │
                         │  Security → FeatureFlag →     │
                         │  VersionRouting → [Route] →   │
                         │  CircuitBreaker → Retry →     │
                         │  Proxy                        │
                         ├──────────┬──────────┬─────────┤
                         │ lb://    │ lb://    │ lb://   │
                         │ deposit  │ transfer │ account │
                         └──────────┴──────────┴─────────┘
```

## Features

### Built-in (Spring Cloud Gateway)
- ✅ **Reverse Proxy** — automatic request forwarding via Netty
- ✅ **Route Matching** — path, method, header predicates in YAML
- ✅ **Circuit Breaker** — Resilience4j per-route with fallback
- ✅ **Retry** — configurable retries with exponential backoff
- ✅ **Load Balancing** — `lb://service-name` with Spring Cloud LoadBalancer
- ✅ **CORS** — declarative in `application.yaml`
- ✅ **Health/Metrics** — Spring Boot Actuator + Gateway endpoints
- ✅ **Rate Limiting** — built-in `RequestRateLimiter` or custom filter

### Custom (our code)
- 🔐 **JWT + API Key Security** — reactive Spring Security with dual auth
- 🚩 **Feature Flags** — dynamic toggle with role + percentage gating
- 🏷️ **Version Routing** — `X-API-Version` header propagation
- 📝 **Access Logging** — structured request/response logging
- ⚡ **Rate Limiting** — Caffeine-backed token bucket filter
- 🛡️ **Error Handling** — consistent JSON error responses

## Endpoints

| Path | Source | Description |
|------|--------|-------------|
| `/actuator/health` | Actuator | Health check |
| `/actuator/gateway/routes` | SCG | All registered routes |
| `/actuator/gateway/globalfilters` | SCG | Active global filters |
| `/actuator/metrics` | Actuator | Application metrics |
| `/gateway/features` | Custom | Feature flag status + toggle |
| `/fallback/{service}` | Custom | Circuit breaker fallback |

## Configuration

All routing is in `application.yaml` — no Java code needed for routes:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: deposit-service
          uri: lb://deposit-service            # load-balanced
          predicates:
            - Path=/api/v1/deposits/**
          filters:
            - name: CircuitBreaker
              args:
                name: deposit-cb
                fallbackUri: forward:/fallback/deposit
            - name: Retry
              args:
                retries: 3
                backoff:
                  firstBackoff: 100ms
                  maxBackoff: 5s
          metadata:
            required-roles: OPERATOR,ADMIN

gateway:
  security:
    jwt:
      enabled: true
      secret: "your-256-bit-secret-key"
    api-key:
      keys:
        your-key:
          name: service-name
          roles: [OPERATOR]
    public-paths:
      - /actuator/**
      - /api/v1/auth/**

  feature-flags:
    flags:
      deposits: { enabled: true, percentage: 100 }
      new-feature: { enabled: false }

  rate-limit:
    enabled: true
    requests-per-minute: 100
```

## Running

```bash
# Build
mvn clean package -pl payment-gateway -DskipTests

# Run
java -jar payment-gateway/target/payment-gateway-0.0.1-SNAPSHOT.jar

# Test
mvn test -pl payment-gateway

# curl
curl localhost:8080/actuator/health
curl -H "X-API-Key: pk_live_admin_2024" localhost:8080/api/v1/deposits
curl -H "Authorization: Bearer <jwt>" localhost:8080/api/v1/deposits
```

## Project Structure

```
src/main/java/org/tars/gateway/
├── GatewayApplication.java              @SpringBootApplication
├── config/
│   ├── GatewayConfig.java               @Configuration
│   ├── GatewaySecurityProperties.java   @ConfigurationProperties
│   └── FeatureFlagProperties.java       @ConfigurationProperties
├── security/
│   ├── JwtService.java                  @Service — JWT validation/generation
│   └── SecurityConfig.java             @EnableWebFluxSecurity — JWT + API key
├── filter/
│   ├── AccessLogFilter.java             GlobalFilter — request logging
│   ├── FeatureFlagFilter.java           GlobalFilter — feature gating
│   ├── VersionRoutingFilter.java        GlobalFilter — X-API-Version
│   ├── RateLimitFilter.java             GlobalFilter — Caffeine token bucket
│   └── FallbackController.java          Circuit breaker fallback
├── feature/
│   ├── FeatureFlagService.java          @Service — flag management
│   └── FeatureFlagController.java       @RestController — CRUD
└── exception/
    └── GatewayExceptionHandler.java     ErrorWebExceptionHandler
```

## Docker

```yaml
# docker-compose.yaml addition
payment-gateway:
  build:
    context: .
    dockerfile: payment-gateway/Dockerfile
  ports:
    - "8080:8080"
  environment:
    - SPRING_PROFILES_ACTIVE=docker
  depends_on:
    - zookeeper
```
