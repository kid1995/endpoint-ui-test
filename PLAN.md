# Service Communication Visualizer & Tester — Implementation Plan

## Context & Analysis

### What We Learned from combine-hint

Your existing microservice (`hint-service`) follows a clean pattern that the visualizer must understand and integrate with:

| Aspect | Pattern in hint-service | Implication for Visualizer |
|--------|------------------------|---------------------------|
| Architecture | Hexagonal (adapters: http, database, message) | Visualizer needs to model adapter-level communication, not just service-level |
| Communication | REST via OpenFeign + Kafka via Spring Cloud Stream | Two distinct edge types in the graph (sync HTTP, async Kafka) |
| Security | OIDC/JWT with `@PreAuthorize` + user allowlist | Visualizer must acquire real JWT tokens from the same OIDC provider |
| Config | Heavy env-var substitution (`${POSTGRES_URL:...}`) | Node config should mirror env-var patterns, not hardcode URLs |
| Client Library | `hint-client` module with `@FeignClient` | Visualizer can reuse existing Feign client interfaces for discovery |
| Kafka Topics | `elpa-hint-created${KAFKA_TOPIC_SUFFIX}` | Topic names are dynamic — visualizer must support suffix-based topic resolution |
| Observability | OpenTelemetry + Micrometer Prometheus | **Key insight**: trace IDs already flow through services — use them instead of building custom tracking |

### New Advice (Beyond Original Prompt)

1. **Use OpenTelemetry traces instead of custom tracking.** Your services already emit OTLP traces. The visualizer should consume these traces (via an OTLP collector or Tempo/Jaeger API) to show real request paths. This is far more accurate than injecting custom "meta-events."

2. **Don't build a Kafka proxy from scratch.** Your services already use Spring Cloud Stream with well-defined bindings. Build a thin "Topic Inspector" that reads from Kafka consumer groups in read-only mode, rather than a proxy that produces messages.

3. **Leverage `@HttpExchange` for mock services.** Spring Boot 4's declarative HTTP interfaces let you generate mock endpoints from your existing Feign client interfaces (`HintClient`). The visualizer's mock services should be auto-generated from these.

4. **Model "Apps" as Kubernetes namespaces/Kustomize overlays.** Your deployment already uses Kustomize overlays (prod/abn/tst). The "App" grouping in the visualizer should map to these environments, not be an arbitrary UI concept.

5. **SSE via `SseEmitter` is the right call for live updates.** But scope it to trace events, not raw HTTP traffic. One SSE stream per active "test session."

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Angular 21 Frontend                   │
│                                                         │
│  ┌──────────┐  ┌──────────┐  ┌────────────────────────┐│
│  │ Graph    │  │ Payload  │  │ Telemetry Stream       ││
│  │ Canvas   │  │ Editor   │  │ (SSE EventSource)      ││
│  │ (SVG)    │  │ (Monaco) │  │                        ││
│  └────┬─────┘  └────┬─────┘  └────────┬───────────────┘│
│       │              │                 │                 │
│  ┌────▼──────────────▼─────────────────▼───────────────┐│
│  │           Signal-based Store                        ││
│  │  graphStore / sessionStore / authStore              ││
│  └─────────────────────┬───────────────────────────────┘│
│                        │ HttpClient + AuthInterceptor   │
└────────────────────────┼────────────────────────────────┘
                         │
        ┌────────────────▼────────────────┐
        │     Visualizer Backend (SB4)    │
        │         Spring MVC              │
        │                                 │
        │  ┌───────────────────────────┐  │
        │  │ GraphController           │  │  CRUD for nodes, edges, apps
        │  │ SessionController         │  │  Test session lifecycle
        │  │ ProxyController           │  │  Forward requests to real services
        │  │ TelemetryController (SSE) │  │  SseEmitter for live updates
        │  │ AuthController            │  │  Token exchange / OIDC flow
        │  └───────────────────────────┘  │
        │                                 │
        │  ┌───────────────────────────┐  │
        │  │ KafkaInspector            │  │  Read-only topic monitoring
        │  │ TraceCollector            │  │  OTLP trace ingestion
        │  │ MockServiceRegistry       │  │  Dynamic mock endpoints
        │  └───────────────────────────┘  │
        └──────────┬──────────┬───────────┘
                   │          │
          ┌────────▼──┐  ┌───▼────────────┐
          │ Real      │  │ Kafka Cluster  │
          │ Services  │  │ (read-only)    │
          │ (hint,etc)│  │                │
          └───────────┘  └────────────────┘
```

---

## Phase Breakdown

### Phase 1: Project Scaffolding & Auth

**Goal:** Working Angular + Spring Boot 4 shell with OIDC authentication.

| Task | Details |
|------|---------|
| 1.1 Init Spring Boot 4 backend | Gradle multi-module: `visualizer-backend`, `visualizer-model`. Spring MVC, no WebFlux. Java 21. |
| 1.2 Init Angular 21 frontend | Standalone components, Signals, Angular Material for layout. |
| 1.3 OIDC integration (backend) | `spring-boot-starter-oauth2-resource-server` + `spring-addons-oidc` (same as hint-service). |
| 1.4 Auth flow (frontend) | Login form → token acquisition → `HttpInterceptor` injects Bearer token on all requests. |
| 1.5 Health check & dev setup | Docker Compose for local Kafka + PostgreSQL (reuse hint-service's compose as base). |

### Phase 2: Graph Data Model & CRUD

**Goal:** Users can create services, edges, and apps. Persisted to DB.

| Task | Details |
|------|---------|
| 2.1 Domain model | `ServiceNode` (name, baseUrl, kafkaTopic, mockResponse, status, position x/y). `ServiceEdge` (source, target, type: HTTP/KAFKA, latency). `App` (name, list of node IDs). |
| 2.2 JPA entities + Flyway migrations | PostgreSQL schema `visualizer`. Immutable DTOs as Java records. MapStruct mappers. |
| 2.3 REST API | `GraphController`: CRUD for nodes, edges, apps. Follows hint-service's pattern (`@PreAuthorize`, `SiErrorMessage` envelope). |
| 2.4 Angular Signal Store | `graphStore` signal: `{ nodes: Signal<ServiceNode[]>, edges: Signal<ServiceEdge[]>, apps: Signal<App[]> }`. |
| 2.5 Graph canvas (SVG) | SVG with draggable circle nodes. Edges as `<path>` with `marker-end="url(#arrowhead)"`. Positions update via signals. |

### Phase 3: Request Proxy & Execution

**Goal:** Send real HTTP requests through the visualizer to target services.

| Task | Details |
|------|---------|
| 3.1 Payload editor | JSON editor panel (Monaco or textarea). Bind to selected node. |
| 3.2 ProxyController | Accepts `{ targetUrl, method, headers, body }`. Forwards via `RestClient` (SB4). Records latency. Returns response + timing. |
| 3.3 Session model | `TestSession` tracks a sequence of proxy calls. Each call = `TransactionEvent` (source, target, status, latencyMs, timestamp, traceId). |
| 3.4 Angular execution flow | "Send" button → proxy call → update edge with latency → animate arrow. |

### Phase 4: Real-Time Telemetry (SSE)

**Goal:** Live updates as requests flow between services.

| Task | Details |
|------|---------|
| 4.1 SseEmitter endpoint | `TelemetryController`: `GET /api/sessions/{id}/events` returns `SseEmitter` (timeout 5min). |
| 4.2 Event publishing | `TelemetryService` holds `Map<sessionId, List<SseEmitter>>`. ProxyController publishes events after each call. |
| 4.3 Angular SSE client | `EventSource` wrapper in `sessionStore`. On event → update edge status/latency signal → SVG re-renders. |
| 4.4 Heartbeat & reconnect | Server sends `:ping` every 15s. Angular auto-reconnects on disconnect. |

### Phase 5: Kafka Integration

**Goal:** Visualize and trigger Kafka messages.

| Task | Details |
|------|---------|
| 5.1 KafkaInspector | Read-only consumer that tails configured topics. Emits events to SSE stream when messages arrive. |
| 5.2 Kafka send (via proxy) | `POST /api/kafka/produce` — produces a message to a topic. Uses `KafkaTemplate` (standard, not reactive). |
| 5.3 Kafka edges in UI | Dashed arrows for async edges. Show topic name on edge label. Animate when message detected. |

### Phase 6: Mock Services & Auto-Discovery

**Goal:** Spin up mock endpoints from Feign client interfaces.

| Task | Details |
|------|---------|
| 6.1 MockServiceRegistry | In-memory registry of mock endpoints. Each mock = path + static JSON response. |
| 6.2 Dynamic mock controller | Catch-all `@RequestMapping` that matches registered mocks and returns configured responses. |
| 6.3 Auto-discovery | Parse `@FeignClient` / `@HttpExchange` interfaces from classpath to suggest node configs. |

---

## Key Technical Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Real-time mechanism | SSE via `SseEmitter` | No WebFlux needed. Native Spring MVC support. Simpler than WebSocket for unidirectional server→client push. |
| Graph rendering | Raw SVG + Angular Signals | No heavy library dependency. SVG `<marker>` for arrows. Signal-driven reactivity for position updates. |
| HTTP forwarding | `RestClient` (SB4) | Modern replacement for RestTemplate. Synchronous. Supports interceptors for tracing. |
| State management | Angular Signals (no NgRx) | Simpler. Sufficient for this scope. Signals are first-class in Angular 21. |
| Kafka integration | `KafkaTemplate` + `@KafkaListener` | Standard Spring Kafka. No Cloud Stream needed for the visualizer itself. |
| Persistence | PostgreSQL + Flyway | Matches your existing stack. Graph configs are relational (nodes, edges, apps). |

---

## File Structure (Planned)

```
endpoint-ui-test/
├── backend/
│   ├── build.gradle                    # Root build (SB4 + plugins)
│   ├── settings.gradle
│   ├── visualizer-model/               # Shared DTOs (Java records)
│   │   └── src/main/java/.../model/
│   └── visualizer-service/             # Main Spring Boot app
│       └── src/main/java/.../
│           ├── VisualizerApplication.java
│           ├── core/
│           │   ├── GraphService.java
│           │   ├── SessionService.java
│           │   └── TelemetryService.java
│           ├── adapter/
│           │   ├── http/
│           │   │   ├── GraphController.java
│           │   │   ├── SessionController.java
│           │   │   ├── ProxyController.java
│           │   │   └── TelemetryController.java
│           │   ├── database/
│           │   │   ├── model/
│           │   │   └── GraphRepository.java
│           │   ├── kafka/
│           │   │   ├── KafkaInspector.java
│           │   │   └── KafkaProducerAdapter.java
│           │   └── mock/
│           │       └── MockServiceRegistry.java
│           └── config/
│               ├── SecurityConfig.java
│               └── CorsConfig.java
├── frontend/
│   ├── src/
│   │   ├── app/
│   │   │   ├── core/
│   │   │   │   ├── auth/              # Login, interceptor, token store
│   │   │   │   └── stores/            # Signal-based stores
│   │   │   ├── features/
│   │   │   │   ├── graph/             # SVG canvas, node/edge components
│   │   │   │   ├── editor/            # Payload editor, config panels
│   │   │   │   └── telemetry/         # SSE stream, latency display
│   │   │   └── shared/                # Common types, utilities
│   │   └── environments/
│   └── angular.json
├── docker-compose.yml                  # PostgreSQL + Kafka (dev)
├── CLAUDE.md
└── PLAN.md
```

---

## Risk Register

| Risk | Mitigation |
|------|------------|
| SSE connections pile up (memory leak) | Timeout after 5min. `SseEmitter.onCompletion()` cleanup. Max 10 concurrent per session. |
| OIDC token for target services differs from visualizer token | ProxyController accepts explicit token override header. UI can paste service-specific tokens. |
| Kafka consumer offset management in inspector | Use unique consumer group per inspection session. Auto-commit disabled. Reset to latest on start. |
| SVG performance with 50+ nodes | Virtual viewport. Only render visible nodes. Batch signal updates with `untracked()`. |
| Mock service conflicts with real routes | Mount mocks under `/api/mocks/*` prefix. Never overlap with real service paths. |
