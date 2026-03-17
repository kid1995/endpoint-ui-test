# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Service Communication Visualizer & Tester** — an interactive tool to simulate, visualize, and test traffic between backend microservices. Built with Angular 21 (frontend) and Spring Boot 4 (backend, Spring MVC only — no WebFlux).

The target microservices follow the pattern established in `combine-hint`: hexagonal architecture, Spring Cloud Stream for Kafka, OpenFeign clients, OIDC/JWT security, PostgreSQL + Flyway, MapStruct, and multi-module Gradle builds.

## Tech Stack

- **Frontend:** Angular 21 — Standalone components, Signals for state (no NgRx), raw SVG for graph rendering
- **Backend:** Spring Boot 4.0.x — Spring MVC (Servlet), Java 21, Gradle multi-module
- **Database:** PostgreSQL + Flyway migrations, Hibernate (ddl-auto: validate)
- **Messaging:** Spring Kafka (`KafkaTemplate` + `@KafkaListener`) — not Spring Cloud Stream for this project
- **Real-time:** SSE via `SseEmitter` (no WebFlux, no WebSocket)
- **Auth:** OIDC/JWT via `spring-addons-oidc` + `oauth2-resource-server`
- **Mapping:** MapStruct for DTO/Entity conversion, Java records for all DTOs

## Build & Run Commands

```bash
# Backend
cd backend
./gradlew build                    # Build all modules
./gradlew :visualizer-service:bootRun  # Run backend (port 8080)
./gradlew test                     # All tests
./gradlew :visualizer-service:test --tests "*.GraphServiceTest"  # Single test class
./gradlew jacocoTestReport         # Coverage report

# Frontend
cd frontend
ng serve                           # Dev server (port 4200)
ng test                            # Unit tests (Karma/Jest)
ng build                           # Production build
ng test --include="**/graph*"      # Run specific test files

# Infrastructure
docker compose up -d               # PostgreSQL + Kafka for local dev
```

## Architecture

```
backend/
├── visualizer-model/        # Shared DTOs (Java records) — published as library
└── visualizer-service/      # Main Spring Boot 4 app
    ├── core/                # Business logic (GraphService, SessionService, TelemetryService)
    ├── adapter/
    │   ├── http/            # REST controllers (Graph, Session, Proxy, Telemetry)
    │   ├── database/        # JPA repositories + entities
    │   ├── kafka/           # KafkaInspector (read-only) + KafkaProducerAdapter
    │   └── mock/            # MockServiceRegistry (dynamic mock endpoints)
    └── config/              # Security, CORS

frontend/src/app/
├── core/
│   ├── auth/                # Login, HttpInterceptor (Bearer token injection)
│   └── stores/              # Signal-based stores (graphStore, sessionStore, authStore)
├── features/
│   ├── graph/               # SVG canvas — draggable nodes, arrow edges with <marker>
│   ├── editor/              # JSON payload editor, node config panel
│   └── telemetry/           # SSE EventSource client, latency display
└── shared/                  # Types, utilities
```

## Key Design Decisions

- **SSE over polling/WebSocket:** `SseEmitter` in Spring MVC provides server→client push without WebFlux. One stream per test session. Timeout 5min with `:ping` heartbeat every 15s.
- **SVG for graph, not a library:** Nodes are `<circle>`, edges are `<path>` with `marker-end="url(#arrowhead)"`. Positions driven by Signals — moving a node updates the signal, SVG re-renders.
- **ProxyController for request forwarding:** UI sends requests through `POST /api/proxy` which forwards to real services via `RestClient`. Records latency and publishes events to SSE.
- **Kafka integration is read-only by default:** `KafkaInspector` tails topics with a unique consumer group. Producing is explicit via `/api/kafka/produce`.
- **Mock services under `/api/mocks/*`:** Dynamic catch-all controller. Never overlap with real service paths.

## Patterns to Follow (Matching combine-hint)

- **Hexagonal/ports-and-adapters:** Business logic in `core/`, all I/O in `adapter/` subpackages
- **Immutable DTOs:** Java records only. Never mutate — create new instances
- **Error envelope:** `SiErrorMessage(String message, String reason)` JSON format for all error responses
- **Global exception handler:** `@ControllerAdvice` mapping exceptions → HTTP status codes
- **Method-level security:** `@PreAuthorize("isAuthenticated() and isAuthorizedUser()")`
- **Env-var config:** All environment-specific values via `${ENV_VAR:default}` in `application.yml`
- **Flyway migrations:** Numbered `V{n}__description.sql`, `baselineOnMigrate: true`, schema isolation
- **MapStruct mappers:** One mapper interface per aggregate. `@Mapper(componentModel = "spring")`
- **Test structure:** Unit tests with Mockito (`@ExtendWith(MockitoExtension.class)`), integration tests with TestContainers + `@WebMvcTest`, 100% branch coverage target (JaCoCo)

## Domain Model

- **ServiceNode:** name, baseUrl, kafkaTopic, mockResponse, status, position (x/y)
- **ServiceEdge:** sourceNodeId, targetNodeId, type (HTTP | KAFKA), latencyMs
- **App:** name, description, list of contained node IDs
- **TestSession:** id, list of TransactionEvents
- **TransactionEvent:** source, target, status, latencyMs, timestamp, traceId

## Important Constraints

- **No WebFlux anywhere** — all controllers return standard types or `SseEmitter`
- **No NgRx** — use Angular Signals directly for state management
- **No heavy graph library** (e.g., D3, Cytoscape) — raw SVG with Signal-driven updates
- **OIDC tokens from target services may differ** — ProxyController accepts explicit token override header
