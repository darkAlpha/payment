# Payment Gateway

**High-performance Netty-based API Gateway** for the Payment System.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Payment Gateway (Netty)                        │
├─────────────────────────────────────────────────────────────────┤
│  Client Request                                                  │
│       │                                                          │
│       ▼                                                          │
│  ┌─────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────────┐ │
│  │  CORS   │→ │Rate Limit│→ │   Auth   │→ │  Authorization   │ │
│  └─────────┘  └──────────┘  └──────────┘  └──────────────────┘ │
│       │                                                          │
│       ▼                                                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────────┐  ┌────────────┐  │
│  │ Feature  │→ │ Version  │→ │Load Balancer │→ │   Proxy    │  │
│  │  Flags   │  │ Routing  │  │              │  │            │  │
│  └──────────┘  └──────────┘  └──────────────┘  └────────────┘  │
│                                      │                           │
├──────────────────────────────────────┼───────────────────────────┤
│                                      ▼                           │
│  ┌───────────┐  ┌───────────┐  ┌───────────┐  ┌─────────────┐  │
│  │  Deposit  │  │ Transfer  │  │  Account  │  │Notification │  │
│  │  Service  │  │  Service  │  │  Service  │  │   Service   │  │
│  └───────────┘  └───────────┘  └───────────┘  └─────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

## Features

### 🔀 Load Balancing
| Strategy | Description |
|----------|-------------|
| `round-robin` | Even distribution across healthy upstreams |
| `weighted` | Traffic proportional to configured weights |
| `random` | Random upstream selection |
| `least-connections` | Routes to upstream with fewest active connections |
| `ip-hash` | Consistent hashing for sticky sessions |

### 🔐 Security
- **JWT Authentication** — Bearer token validation with role claims
- **API Key Authentication** — `X-API-Key` header with per-key roles and path restrictions
- **RBAC** — Role-based access control with path-level permissions
- **CORS** — Configurable cross-origin resource sharing

### 🚦 Traffic Management
- **Rate Limiting** — Per-client and per-path limits (Resilience4j)
- **Circuit Breaker** — Per-route circuit breaking with configurable thresholds
- **Retry with Backoff** — Exponential backoff on upstream failures
- **Request Timeout** — Per-route configurable timeouts

### 🏷️ API Versioning
- **Header-based** — Route by `X-API-Version` header value
- **Percentage-based** — Canary/blue-green deployments with traffic split

### 🚩 Feature Flags
- Enable/disable features dynamically at runtime
- Role-based feature access
- Percentage-based rollout

### 📊 Observability
- Access logging with request ID correlation
- Request metrics (total, errors, latency, by status/path)
- Health check endpoint

## Endpoints

| Path | Description |
|------|-------------|
| `/health` | Health check |
| `/gateway/health` | Detailed health with JVM info |
| `/gateway/metrics` | Request metrics |
| `/gateway/features` | Feature flags status |
| `/gateway/routes` | Configured routes |

## Configuration

Configuration is loaded from `gateway.yaml` (classpath or external file):

```yaml
server:
  port: 8080
  bossThreads: 1
  workerThreads: 0  # auto

security:
  jwt:
    enabled: true
    secret: "your-256-bit-secret"
  apiKey:
    enabled: true
    keys:
      "your-api-key":
        name: "service-name"
        roles: ["OPERATOR"]
        allowedPaths: ["/api/**"]

routes:
  - id: "deposit-service"
    path: "/api/v1/deposits/**"
    loadBalancer: "round-robin"
    upstreams:
      - url: "http://localhost:8081"
        weight: 3
        version: "v1"
    versioning:
      strategy: "percentage"
      percentages:
        v1: 80
        v2: 20
```

## Running

```bash
# Build
cd payment-gateway
mvn clean package -DskipTests

# Run
java -jar target/payment-gateway-0.0.1-SNAPSHOT.jar

# Or with external config
java -jar target/payment-gateway-0.0.1-SNAPSHOT.jar -Dconfig=./gateway.yaml
```

## Testing

```bash
# Run all tests
mvn test

# Test with curl
curl http://localhost:8080/health

# Authenticated request (JWT)
curl -H "Authorization: Bearer <token>" http://localhost:8080/api/v1/deposits

# Authenticated request (API Key)
curl -H "X-API-Key: pk_live_deposit_service_key_2024" http://localhost:8080/api/v1/deposits

# Version routing
curl -H "X-API-Version: v2" -H "X-API-Key: ..." http://localhost:8080/api/v1/deposits
```

## Filter Chain Order

| Order | Filter | Description |
|-------|--------|-------------|
| -100 | CORS | Handle preflight & CORS headers |
| -50 | Request ID | Assign/propagate X-Request-Id |
| 0 | Access Log | Log request/response |
| 100 | Rate Limit | Throttle excessive requests |
| 200 | Authentication | Validate JWT/API key |
| 300 | Authorization | Enforce RBAC |
| 400 | Feature Flag | Block disabled features |
| 700 | Route Resolve | Match route + load balance |
| 900 | Proxy | Forward to upstream |

## Adding Custom Filters

```java
public class MyCustomFilter implements GatewayFilter {
    @Override public String name() { return "my-filter"; }
    @Override public int order() { return 350; } // after auth, before feature flags
    @Override public void filter(GatewayContext context, GatewayFilterChain chain) {
        // Pre-processing
        context.setAttribute("custom-data", "value");
        
        chain.next(context); // Continue chain
        
        // Post-processing
        context.addResponseHeader("X-Custom", "value");
    }
}
```

## Adding Custom Load Balancer

```java
public class MyStrategy implements LoadBalancerStrategy {
    @Override public String name() { return "my-strategy"; }
    @Override public UpstreamConfig select(List<UpstreamConfig> upstreams, String key) {
        // Custom selection logic
    }
}

// Register
LoadBalancerFactory.register(new MyStrategy());
```

## Project Structure

```
payment-gateway/
├── src/main/java/org/tars/gateway/
│   ├── GatewayApplication.java          # Entry point
│   ├── server/
│   │   ├── NettyGatewayServer.java      # Netty bootstrap
│   │   ├── GatewayServerInitializer.java # Channel pipeline
│   │   └── GatewayRequestHandler.java   # Request dispatch
│   ├── config/
│   │   ├── GatewayConfig.java           # Configuration model
│   │   └── GatewayConfigLoader.java     # YAML loader
│   ├── context/
│   │   └── GatewayContext.java          # Request context
│   ├── filter/
│   │   ├── GatewayFilter.java           # Filter interface
│   │   ├── GatewayFilterChain.java      # Chain execution
│   │   ├── FilterOrder.java             # Order constants
│   │   ├── pre/                         # Pre-proxy filters
│   │   └── post/                        # Post-proxy filters
│   ├── security/
│   │   ├── AuthenticationProvider.java
│   │   ├── JwtAuthProvider.java
│   │   ├── ApiKeyAuthProvider.java
│   │   └── rbac/                        # RBAC model
│   ├── loadbalancer/
│   │   ├── LoadBalancerStrategy.java
│   │   ├── LoadBalancerFactory.java
│   │   └── [strategies]
│   ├── versioning/
│   │   ├── VersionRouter.java
│   │   ├── HeaderVersionStrategy.java
│   │   └── PercentageVersionStrategy.java
│   ├── feature/
│   │   └── FeatureFlagManager.java
│   ├── proxy/
│   │   ├── ProxyClient.java
│   │   └── ProxyResponse.java
│   ├── health/
│   ├── metrics/
│   └── exception/
└── src/main/resources/
    ├── gateway.yaml                     # Configuration
    └── logback.xml                      # Logging config
```

