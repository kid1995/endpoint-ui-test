# Service Communication Visualizer & Tester

A visual tool to simulate, test, and track traffic between backend microservices. Built for testing services like `hint-service` through both HTTP REST and Kafka messaging — with real-time telemetry via SSE.

## Architecture

```
┌──────────────────────┐     ┌──────────────────────┐     ┌──────────────────────┐
│  Angular 21 UI       │     │  Visualizer Backend   │     │  Fake Hint Caller    │
│  localhost:4200      │────▶│  localhost:8080       │     │  localhost:8084       │
│                      │     │                       │     │                       │
│  - Graph Canvas(SVG) │ SSE │  - GraphController    │     │  - HintRestCaller     │
│  - Node Config Panel │◀────│  - ProxyController    │     │  - HintKafkaProducer  │
│  - Telemetry Panel   │     │  - TelemetryController│     │  - ScenarioController │
│  - Auth (JWT)        │     │  - KafkaInspector     │     │  - PreloadedScenarios │
└──────────────────────┘     └──────────┬───────────┘     └──────────┬───────────┘
                                        │                            │
                             ┌──────────▼───────────┐     ┌──────────▼───────────┐
                             │  hint-service         │     │  Kafka (KRaft)       │
                             │  localhost:8082       │◀────│  localhost:9092       │
                             │                       │     │  topic:              │
                             │  - GET  /api/hints    │     │  elpa-hint-created   │
                             │  - GET  /api/hints/id │     └──────────────────────┘
                             │  - POST /api/hints    │
                             └───────────────────────┘
```

## Prerequisites

- Java 21+
- Node.js 20+
- Docker & Docker Compose
- GNU Make (optional, pre-installed on macOS/Linux)

## Fast Commands

All operations support targeting `all`, `backend`/`be`, `frontend`/`fe`, or `infra`.

### Via Make (recommended)

```bash
make install          # Install everything (infra + backend + frontend)
make install-be       # Backend only (Gradle build, skip tests)
make install-fe       # Frontend only (npm install)
make install-infra    # Start PostgreSQL + Kafka containers

make start            # Start all services
make start-be         # Start both backend services
make start-fe         # Start Angular dev server
make start-infra      # Start containers only

make clean            # Clean everything (build artifacts + node_modules + containers + volumes)
make clean-be         # Clean backend build artifacts
make clean-fe         # Clean frontend (node_modules, dist, .angular)
make clean-infra      # Stop containers and remove volumes

make stop             # Stop all containers
make status           # Show container status and ports
make help             # Show all available commands
```

### Via Shell Scripts

```bash
./scripts/install.sh all        # or: backend, frontend, infra
./scripts/start.sh all          # or: backend, visualizer, fake, frontend, infra
./scripts/clean.sh all          # or: backend, frontend, infra
```

### One-Liner: From Zero to Running

```bash
make install && make start
```

### One-Liner: Full Reset

```bash
make clean && make install && make start
```

## Quick Start (Manual)

### 1. Start Infrastructure + hint-service

```bash
docker compose up -d
```

This starts:
| Service        | Port | Description                              |
|----------------|------|------------------------------------------|
| PostgreSQL     | 5432 | Database (user: `db_user` / `db_password`) |
| Kafka (KRaft)  | 9092 | Message broker                           |
| hint-service   | 8082 | The target service under test             |

### 2. Start Visualizer Backend

```bash
cd backend
./gradlew :visualizer-service:bootRun
```

Runs on `http://localhost:8080`. Provides graph CRUD, request proxy, SSE telemetry, and Kafka inspection.

### 3. Start Fake Hint Caller

```bash
cd backend
./gradlew :fake-hint-caller:bootRun
```

Runs on `http://localhost:8084`. Provides pre-built test scenarios that call hint-service.

### 4. Start Angular UI

```bash
cd frontend
npm install
ng serve
```

Open `http://localhost:4200`.

## Using the UI

### Authentication

1. Paste a valid Bearer token into the header input and click **Login**
2. The token is automatically attached to all outgoing requests via the `HttpInterceptor`
3. For local development with security disabled (`SECURITY_ENABLED=false`), the token is passed through to hint-service via the `X-Target-Token` header

### Building the Service Graph

1. Click **+ Add Node** to create a service node on the canvas
2. Click a node to select it — the **Node Configuration** panel appears on the right
3. Configure the node:
   - **Name**: e.g., `hint-service`
   - **Base URL**: `http://localhost:8082/api`
   - **Kafka Topic**: `elpa-hint-created` (for Kafka-enabled services)
   - **Status**: ONLINE / OFFLINE / DEGRADED / UNKNOWN
4. Click **Save**
5. Drag nodes to arrange them on the canvas
6. Edges (arrows) represent communication paths:
   - **Solid blue arrow** = HTTP connection
   - **Dashed purple arrow** = Kafka connection
   - Latency (ms) is displayed on the edge after a request completes

### Sending Test Requests from the UI

#### HTTP Request

1. Select a node (e.g., `hint-service`)
2. In the **Send HTTP Request** section:
   - **Method**: `GET`
   - **Path**: `/hints`
3. Click **Send Request**
4. The response (status code, body, latency) appears below
5. The **Telemetry** panel on the right shows the live event

#### Kafka Message

1. Select a node that has a Kafka Topic configured
2. In the **Send Kafka Message** section, enter a JSON payload:
   ```json
   {
     "hintSource": "MANUAL-TEST",
     "message": "Testing Kafka flow",
     "hintCategory": "INFO",
     "showToUser": true,
     "processId": "test-001",
     "creationDate": "2026-03-17T10:00:00"
   }
   ```
3. Click **Produce Message**
4. The KafkaInspector detects the message and pushes an event to the Telemetry panel

## Test Scenarios (via Fake Hint Caller)

The fake-hint-caller service at `http://localhost:8084` provides ready-made test scenarios. You can call these from the UI proxy or directly via curl.

---

### Scenario 1: Save hints via REST and retrieve them

**Purpose**: Test `POST /api/hints` and `GET /api/hints` on hint-service.

```bash
# Step 1: Save hints (batch of 3)
curl -X POST http://localhost:8084/api/scenarios/http/save-hints \
  -H "Content-Type: application/json" \
  -H "X-Target-Token: <your-jwt-token>" \
  -d '[
    {
      "hintSource": "FAKE-SERVICE",
      "message": "Partner validation failed",
      "hintCategory": "ERROR",
      "showToUser": true,
      "processId": "process-100",
      "creationDate": "2026-03-17T10:00:00"
    },
    {
      "hintSource": "FAKE-SERVICE",
      "message": "Address check passed",
      "hintCategory": "INFO",
      "showToUser": true,
      "processId": "process-100",
      "creationDate": "2026-03-17T10:00:01"
    },
    {
      "hintSource": "FAKE-SERVICE",
      "message": "Duplicate detected",
      "hintCategory": "WARNING",
      "showToUser": false,
      "processId": "process-100",
      "creationDate": "2026-03-17T10:00:02"
    }
  ]'

# Step 2: Retrieve them
curl http://localhost:8084/api/scenarios/http/get-hints?processId=process-100 \
  -H "X-Target-Token: <your-jwt-token>"
```

**Expected**: Step 1 returns `201 CREATED`. Step 2 returns the 3 hints.

---

### Scenario 2: Send hint via Kafka and verify it was consumed

**Purpose**: Test hint-service's Spring Cloud Stream consumer (`hintCreated` function bean).

```bash
# Step 1: Produce a hint message to Kafka
curl -X POST http://localhost:8084/api/scenarios/kafka/send-hint \
  -H "Content-Type: application/json" \
  -d '{
    "hintSource": "UPSTREAM-SERVICE",
    "message": "Kafka consumer test",
    "hintCategory": "INFO",
    "showToUser": true,
    "processId": "kafka-test-001"
  }'

# Step 2: Wait 2 seconds for hint-service to consume, then query via REST
curl http://localhost:8084/api/scenarios/http/get-hints?processId=kafka-test-001 \
  -H "X-Target-Token: <your-jwt-token>"
```

**Expected**: Step 1 returns `{ "success": true, "topic": "elpa-hint-created" }`. Step 2 returns the hint that was consumed from Kafka and saved to DB.

---

### Scenario 3: Full round-trip (Kafka → hint-service → REST verify)

**Purpose**: End-to-end test in a single call. Sends via Kafka, waits, then queries REST.

```bash
curl -X POST http://localhost:8084/api/scenarios/full-flow/kafka-then-rest \
  -H "Content-Type: application/json" \
  -H "X-Target-Token: <your-jwt-token>" \
  -d '{
    "hintSource": "E2E-TEST",
    "message": "Full round-trip test",
    "hintCategory": "WARNING",
    "processId": "e2e-test-001",
    "waitMs": 3000
  }'
```

**Expected response**:
```json
{
  "kafkaResult": {
    "topic": "elpa-hint-created",
    "success": true,
    "latencyMs": 45
  },
  "restResult": {
    "endpoint": "GET /hints",
    "statusCode": 200,
    "body": "[{\"hintSource\":\"E2E-TEST\", ...}]",
    "latencyMs": 12,
    "success": true
  }
}
```

---

### Scenario 4: Test all 4 hint categories at once

**Purpose**: Verify hint-service handles INFO, WARNING, ERROR, and BLOCKER correctly.

```bash
# Via REST
curl -X POST http://localhost:8084/api/scenarios/preloaded/rest-all-categories \
  -H "X-Target-Token: <your-jwt-token>"

# Via Kafka
curl -X POST http://localhost:8084/api/scenarios/preloaded/kafka-all-categories
```

**Expected**: Returns an array of 4 results — one per category, all successful.

---

### Scenario 5: Test error handling

**Purpose**: Verify hint-service returns proper error responses for bad input.

```bash
curl -X POST http://localhost:8084/api/scenarios/preloaded/error-handling \
  -H "X-Target-Token: <your-jwt-token>"
```

**Expected response**:
```json
{
  "empty_fields": {
    "endpoint": "POST /hints",
    "statusCode": 400,
    "success": false
  },
  "malformed_json": {
    "endpoint": "POST /hints",
    "statusCode": 400,
    "success": false
  },
  "no_auth": {
    "endpoint": "GET /hints",
    "statusCode": 401,
    "success": false
  }
}
```

---

### Scenario 6: Search with different query parameter combinations

**Purpose**: Test hint-service's JPA Specification-based query filtering.

```bash
curl http://localhost:8084/api/scenarios/preloaded/search-combinations \
  -H "X-Target-Token: <your-jwt-token>"
```

**Expected**: Returns results for `by_process_id`, `by_source_prefix`, and `by_category` queries — testing the optimized processId path, the prefix-matching path, and the Specification-based path.

---

### Scenario 7: Kafka throughput test

**Purpose**: Send N messages via Kafka in rapid succession, then verify hint-service consumed them.

```bash
# Send 20 hints via Kafka, then verify
curl -X POST "http://localhost:8084/api/scenarios/preloaded/throughput-test?count=20" \
  -H "X-Target-Token: <your-jwt-token>"
```

**Expected**:
```json
{
  "kafkaBatch": {
    "total": 20,
    "success": 20,
    "failed": 0,
    "totalLatencyMs": 850
  },
  "verificationQuery": {
    "statusCode": 200,
    "success": true
  }
}
```

---

### Scenario 8: Kafka error payloads

**Purpose**: Test what happens when hint-service receives malformed Kafka messages.

```bash
curl -X POST http://localhost:8084/api/scenarios/preloaded/kafka-error-handling
```

**Expected**: Messages are produced successfully (Kafka accepts any bytes), but hint-service's consumer will log deserialization errors. Check hint-service logs: `docker compose logs hint-service`.

---

### Scenario 9: Send custom raw payload via Kafka

**Purpose**: Send any arbitrary JSON to test edge cases.

```bash
curl -X POST http://localhost:8084/api/scenarios/kafka/send-raw \
  -H "Content-Type: application/json" \
  -d '{
    "key": "custom-key",
    "payload": "{\"hintSource\":\"RAW\",\"message\":\"Custom payload\",\"hintCategory\":\"BLOCKER\",\"showToUser\":true,\"processId\":\"raw-001\",\"creationDate\":\"2026-03-17T12:00:00\"}"
  }'
```

---

## Telemetry (Real-Time SSE)

The Visualizer Backend pushes live events to the Angular UI via Server-Sent Events (SSE):

- **No WebFlux** — uses `SseEmitter` from Spring MVC
- **Heartbeat** every 15 seconds (`:ping` comment)
- **Auto-reconnect** in Angular if connection drops (3-second retry)
- **Event types**: `HTTP_REQUEST`, `HTTP_RESPONSE`, `KAFKA_PRODUCE`, `KAFKA_CONSUME`

Connect manually for debugging:
```bash
curl -N http://localhost:8080/api/telemetry/my-session/events
```

Then trigger a proxy request:
```bash
curl -X POST http://localhost:8080/api/proxy/http \
  -H "Content-Type: application/json" \
  -d '{
    "targetUrl": "http://localhost:8082/api/hints",
    "method": "GET",
    "sessionId": "my-session",
    "sourceNodeName": "visualizer",
    "targetNodeName": "hint-service"
  }'
```

You'll see events appear in the SSE stream:
```
event: HTTP_REQUEST
data: {"id":"...","sourceNode":"visualizer","targetNode":"hint-service","status":"IN_FLIGHT",...}

event: HTTP_RESPONSE
data: {"id":"...","sourceNode":"hint-service","targetNode":"visualizer","status":"SUCCESS","latencyMs":23,...}
```

## Port Reference

| Service              | API Port | Management Port | Description                  |
|----------------------|----------|-----------------|------------------------------|
| Visualizer Backend   | 8080     | 8081            | Graph, proxy, SSE, Kafka     |
| hint-service         | 8082     | 8083            | Target service under test    |
| Fake Hint Caller     | 8084     | 8085            | Test scenario runner         |
| PostgreSQL           | 5432     | —               | Database                     |
| Kafka                | 9092     | —               | Message broker               |
| Angular UI           | 4200     | —               | Frontend dev server          |

## Environment Variables

### Visualizer Backend

| Variable                 | Default                   | Description                    |
|--------------------------|---------------------------|--------------------------------|
| `POSTGRES_HOST`          | `localhost`               | PostgreSQL host                |
| `POSTGRES_PORT`          | `5432`                    | PostgreSQL port                |
| `POSTGRES_DB_NAME`       | `visualizer`              | Database name                  |
| `POSTGRES_USER`          | `db_user`                 | Database user                  |
| `POSTGRES_PASSWORD`      | `db_password`             | Database password              |
| `KAFKA_BROKERS`          | `localhost:9092`          | Kafka bootstrap servers        |
| `KAFKA_INSPECT_TOPICS`   | `elpa-hint-created`       | Topics for KafkaInspector      |
| `SECURITY_ENABLED`       | `false`                   | Enable OIDC JWT validation     |
| `CORS_ORIGINS`           | `http://localhost:4200`   | Allowed CORS origins           |

### Fake Hint Caller

| Variable                 | Default                          | Description               |
|--------------------------|----------------------------------|---------------------------|
| `HINT_SERVICE_URL`       | `http://localhost:8082/api`      | hint-service base URL     |
| `KAFKA_BROKERS`          | `localhost:9092`                 | Kafka bootstrap servers   |
| `KAFKA_HINT_TOPIC`       | `elpa-hint-created`              | Kafka topic to produce to |

## Tech Stack

- **Frontend**: Angular 21, Standalone Components, Signals, SVG
- **Backend**: Spring Boot 4.0.3, Spring MVC (no WebFlux), Java 21
- **Database**: PostgreSQL 18 + Flyway
- **Messaging**: Kafka 4.2 (KRaft mode)
- **Real-time**: SSE via `SseEmitter`
- **Mapping**: MapStruct 1.6.3
