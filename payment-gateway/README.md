# Payment Gateway

**High-performance Netty-based API Gateway** with Spring Boot integration.

## Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                 Payment Gateway (Spring Boot + Netty)              │
├──────────────────────────────────────────────────────────────────┤
│  ┌──────┐   ┌──────────┐   ┌──────┐   ┌──────────────────┐      │
│  │ CORS │ → │Rate Limit│ → │ Auth │ → │  Authorization   │      │
│  └──────┘   └──────────┘   └──────┘   └──────────────────┘      │
│       │                                                           │
│       ▼                                                           │
│  ┌──────────┐  ┌──────────┐  ┌──────────────┐  ┌────────────┐   │
│  │ Feature  │→ │ Version  │→ │Load Balancer │→ │   Proxy    │   │
│  │  Flags   │  │ Routing  │  │  (5 strats)  │  │ + CB/Retry │   │
│  └──────────┘  └──────────┘  └──────────────┘  └────────────┘   │
├──────────────────────────────────────────────────────────────────┤
│  Upstream Services: Deposit | Transfer | Account | Notification   │
└──────────────────────────────────────────────────────────────────┘
```

## Spring Boot Best Practices Applied

| Practice | Implementation |
|----------|---------------|
| **`@ConfigurationProperties`** | Type-safe `GatewayProperties` with `@Validated` |
| **Component Scanning** | All filters are `@Component` beans, auto-discovered |
| **Conditional Beans** | `@ConditionalOnProperty` for rate-limit, feature-flags |
| **Lifecycle Management** | `@EventListener(ApplicationReadyEvent)` + `@PreDestroy` |
| **Bean Composition** | `@Configuration` classes wire LB strategies, registries |
| **Spring Boot Testing** | `spring-boot-starter-test` with JUnit 5 + AssertJ |
| **External Config** | `application.yaml` with Spring relaxed binding |
| **Profiles** | `application-test.yaml` for isolated testing |
| **Lombok** | `@Data`, `@Slf4j` for clean POJOs and logging |

## Features

### 🔀 Load Balancing (5 strategies)
- `round-robin` — even distribution
- `weighted` — proportional to weight
- `random` — random selection
- `least-connections` — routes to least-busy
- `ip-hash` — consistent hashing (sticky sessions)

### 🔐 Security
- **JWT** (`Authorization: Bearer <token>`) with role extraction
- **API Key** (`X-API-Key`) with per-key roles + path restrictions
- **RBAC** — role-based path access (ADMIN, OPERATOR, VIEWER, SERVICE)
- **CORS** — configurable cross-origin support

### 🚦 Traffic Control
- **Rate Limiting** — Resilience4j per-client/path
- **Circuit Breaker** — per-route with configurable thresholds
- **Retry** — exponential backoff
- **Timeouts** — per-route configurable

### 🏷️ API Versioning
- **Header** — `X-API-Version: v2`
- **Percentage** — canary deploys (v1=80%, v2=20%)

### 🚩 Feature Flags
- Enable/disable at runtime
- Role-gated + percentage rollout
- `@ConditionalOnProperty` toggleable

### 📊 Observability
- Access log with request ID correlation
- `/gateway/health` — JVM + uptime info
- `/gateway/metrics` — request counts, latency, error rates
- `/gateway/features` — flag status
- `/gateway/routes` — registered routes

## Configuration (`application.yaml`)

```yaml
gateway:
  server:
    port: 8080
    boss-threads: 1
    worker-threads: 0  # auto

  security:
    jwt:
      enabled: true
      secret: "your-256-bit-secret-key"
    api-key:
      enabled: true
      keys:
        your-key-here:
          name: service-name
          roles: [OPERATOR]
          allowed-paths: ["/api/**"]
    public-paths:
      - /health
      - /api/v1/auth/**

  routes:
    - id: deposit-service
      path: /api/v1/deposits/**
      load-balancer: round-robin
      required-roles: [OPERATOR, ADMIN]
      timeout-ms: 30000
      retries: 2
      upstreams:
        - url: http://localhost:8081
          weight: 3
          version: v1
      versioning:
        strategy: percentage
        percentages: { v1: 80, v2: 20 }
      circuit-breaker:
        failure-rate-threshold: 50
        sliding-window-size: 10
```

## Running

```bash
# Build
mvn clean package -pl payment-gateway -DskipTests

# Run
java -jar payment-gateway/target/payment-gateway-0.0.1-SNAPSHOT.jar

# With profile
java -jar target/*.jar --spring.profiles.active=prod
```

## Testing

```bash
mvn test -pl payment-gateway

# Manual testing
curl localhost:8080/health
curl -H "X-API-Key: pk_live_deposit_service_2024" localhost:8080/api/v1/deposits
curl -H "Authorization: Bearer <token>" -H "X-API-Version: v2" localhost:8080/api/v1/deposits
```

## Filter Chain (ordered)

| Order | Filter | Condition |
|-------|--------|-----------|
| -100 | CorsFilter | always |
| -50 | RequestIdFilter | always |
| 0 | AccessLogFilter | `access-log-enabled=true` |
| 100 | RateLimitFilter | `rate-limit.enabled=true` |
| 200 | AuthenticationFilter | always (skips public paths) |
| 300 | AuthorizationFilter | always |
| 400 | FeatureFlagFilter | `feature-flags.enabled=true` |
| 700 | RouteResolveFilter | always |
| 900 | ProxyFilter | always |

## Adding a Custom Filter

```java
@Component
public class MyFilter implements GatewayFilter {
    @Override public String getName() { return "my-filter"; }
    @Override public int getOrder() { return 350; }

    @Override
    public void filter(GatewayContext context, GatewayFilterChain chain) {
        // pre-logic
        chain.next(context);
        // post-logic
    }
}
```

## Adding a Custom Load Balancer

```java
// 1. Implement strategy
public class MyLbStrategy implements LoadBalancerStrategy { ... }

// 2. Register as bean in LoadBalancerConfiguration
@Bean
public MyLbStrategy myLbStrategy() { return new MyLbStrategy(); }

// 3. Use in route config
gateway.routes[0].load-balancer=my-strategy
```

## Project Structure

```
src/main/java/org/tars/gateway/
├── GatewayApplication.java              @SpringBootApplication
├── config/
│   ├── GatewayProperties.java           @ConfigurationProperties
│   ├── GatewayConfiguration.java        @Configuration (core beans)
│   └── LoadBalancerConfiguration.java   @Configuration (LB beans)
├── server/
│   ├── NettyGatewayServer.java          @Component (lifecycle)
│   ├── GatewayChannelInitializer.java   Netty pipeline
│   └── GatewayRequestHandler.java       Request dispatch
├── context/
│   └── GatewayContext.java              Per-request state
├── filter/
│   ├── GatewayFilter.java              Interface
│   ├── GatewayFilterChain.java         Chain executor
│   ├── FilterOrder.java                Constants
│   ├── pre/                            @Component filters
│   └── post/                           @Component filters
├── security/
│   ├── AuthenticationResult.java       Record
│   ├── jwt/JwtAuthProvider.java        @Component
│   ├── apikey/ApiKeyAuthProvider.java  @Component
│   └── rbac/RbacService.java          Bean
├── loadbalancer/                       Strategy pattern
├── versioning/                         Header + Percentage
├── feature/FeatureFlagService.java     Bean
├── proxy/                              Netty HTTP client
├── metrics/MetricsService.java         @Service
├── health/HealthService.java           @Service
└── exception/GatewayException.java
```
