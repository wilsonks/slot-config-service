# slot-config-service

Centralized externalized configuration service for the `slot-central` microservices platform. Built as a **Spring Cloud Config Server** (native profile, classpath-backed), it serves per-service YAML configs and a DB-backed feature-flag API with audit trail.

Part of the re-architecture of the `slot-central-server-express-rmq` Node.js EGM slot-floor backend into Spring Boot microservices.

---

## Role in the Platform

The monolith had no centralized config service — configuration was hardcoded in each module (e.g. bet/denom value tables in `game-service`, jackpot contribution rate as literal constants). This service fixes that anti-pattern: a single source of truth for tunable game/business parameters that other services fetch at startup and can refresh at runtime without redeployment.

**This service does NOT own:**
- Game math (slot-game-engine-service)
- Wallet/bank state (slot-bank-service)
- Floor/station state (slot-floor-management-service)
- Jackpot pool state (slot-jackpot-service)

**Intended future consumers (not modified by this task — follow-up integration work):**
- `slot-game-engine-service` — should fetch paytable, reel-strip, bonus config, and `gaffing.enabled` from this service instead of hardcoding it
- `slot-jackpot-service` — should fetch `incrementRate`/`baseAmount` defaults per tier from this service

---

## Design Decisions

### 1. Native Profile vs Git-Backed Config Store

**Choice: Native (classpath) profile** — config YAML files live in `src/main/resources/config-repo/` and are bundled into the JAR.

**Why:** For a self-contained dev/demo setup, native profile requires no git tooling, no external repository URL, and works identically in all environments including CI and Docker. The bundled files ship with the service and can be edited and rebuilt like any source file.

**Tradeoff:** Config changes require a service rebuild and redeploy. For production environments where config needs to change without redeployment, switch to git-backed:

```yaml
# application.yml — switch to git-backed:
spring:
  profiles:
    active: git   # instead of: native
  cloud:
    config:
      server:
        git:
          uri: ${CONFIG_GIT_URI}          # e.g. https://github.com/org/slot-config-repo
          default-label: ${CONFIG_GIT_BRANCH:main}
          search-paths: '{application}'   # optional: subdirectory per service
          clone-on-start: true
          force-pull: true
```

Set `CONFIG_GIT_URI` environment variable to your external config repository URL. With git-backed, a `POST /actuator/refresh` call to a consumer service (or Spring Cloud Bus broadcast) picks up the new config from the git repo without redeployment.

### 2. Hybrid Config Architecture: Git/Classpath + DB-backed Flags

**Slow-changing structural config (paytable, reel strips, bet/denom tables):** Stored as YAML in `config-repo/` — served via the Spring Cloud Config Server endpoints. Changes require a code review, rebuild, and deploy, which is appropriate for regulated game math parameters that should never change casually.

**Fast-toggle operational flags (e.g. `gaffing.enabled`):** Stored in a Postgres `feature_flags` table — served via the custom `/api/v1/flags` REST API. Changes take effect immediately without any rebuild or redeploy. Every change is written to `feature_flag_audit_log` for compliance.

**Why a DB for fast flags?** A git commit round-trip (edit file → commit → push → Config Server polls → consumer refreshes) takes 30–120 seconds and leaves an unstructured git history for operational toggles. A database write is instant, transactional, and the audit log table gives regulators a clean, queryable record of who changed what and when — exactly what gaming compliance requires for a `gaffing`-type administrative control.

---

## Bundled Example Config

### `config-repo/application.yml` (shared defaults)
Common actuator, logging, and platform version settings inherited by all services.

### `config-repo/game-engine-service.yml`
Reflects the paytable/bet/denom/reel/bonus config currently hardcoded in `slot-game-engine-service`:

| Section | Description |
|---------|-------------|
| `game.engine.gaffing.enabled` | Administrative RTP adjustment flag (default: `false`). DB-backed flag takes precedence at runtime. |
| `game.engine.gaffing.rtpAdjustment` | RTP adjustment delta when gaffing is enabled (default: `0.0`) |
| `game.engine.betsValues` | 10-tier bet multiplier array mirroring the monolith's `betsValues` constant |
| `game.engine.denomValues` | 9-tier denomination array mirroring the monolith's `denomValues` constant |
| `game.engine.paytable.credits` | Credit awards for 3-of-a-kind, 4-of-a-kind, 5-of-a-kind per symbol (wild, 7, bar3, bar2, bar1, cherry) |
| `game.engine.reelStrips.reel1–5` | 20-stop reel strip definitions for a 5-reel slot |
| `game.engine.bonus` | Scatter trigger config, free spins count/multiplier, picker bonus prize table |

### `config-repo/jackpot-service.yml`
Default jackpot pool parameters for each tier:

| Tier | baseAmount | incrementRate | triggerOdds |
|------|-----------|---------------|-------------|
| mini | $10.00 | 0.1% | 5% |
| minor | $100.00 | 0.2% | 1% |
| major | $1,000.00 | 0.3% | 0.1% |
| grand | $10,000.00 | 0.5% | 0.01% |

---

## Feature Flag API

All endpoints require a valid JWT from `slot-auth-service`. `PUT` requires `ROLE_ADMIN` or `ROLE_STAFF`.

### List all flags
```
GET /api/v1/flags
Authorization: ******
```

### Get a flag
```
GET /api/v1/flags/{flagName}
Authorization: ******
```

**Response:**
```json
{
  "name": "gaffing.enabled",
  "value": "false",
  "description": "Administrative RTP adjustment toggle",
  "updatedAt": "2024-01-15T10:30:00",
  "updatedBy": "admin@slotcentral.com"
}
```

### Toggle / create a flag
```
PUT /api/v1/flags/{flagName}
Authorization: ******
Content-Type: application/json

{
  "value": "true",
  "updatedBy": "admin@slotcentral.com"
}
```

### Get audit log for a flag
```
GET /api/v1/flags/{flagName}/audit
Authorization: ******
```

**Response:**
```json
[
  {
    "flagName": "gaffing.enabled",
    "oldValue": "false",
    "newValue": "true",
    "changedBy": "admin@slotcentral.com",
    "changedAt": "2024-01-15T10:30:00"
  }
]
```

---

## Audit Log for Compliance

Every `PUT /api/v1/flags/{flagName}` call writes an immutable row to `feature_flag_audit_log` containing `flagName`, `oldValue`, `newValue`, `changedBy`, and `changedAt`. This table is append-only — no updates or deletes are issued to it. This provides regulators with a tamper-evident record of all administrative toggles (especially `gaffing.enabled`, which directly affects RTP, making it a compliance-sensitive parameter in most gaming jurisdictions).

---

## Spring Cloud Config Server Endpoints

The standard Spring Cloud Config endpoints are permit-all (protected by network boundary — not exposed publicly):

```
GET /{application}/{profile}        → JSON property source for that application+profile
GET /{application}/{profile}/{label} → same, pinned to a git label (native: ignored)
GET /{application}-{profile}.yml    → YAML representation
```

Examples:
```bash
curl http://localhost:8888/game-engine-service/default
curl http://localhost:8888/jackpot-service/production
curl http://localhost:8888/game-engine-service-default.yml
```

---

## Security

JWT OAuth2 Resource Server validating against `slot-auth-service` JWKS endpoint.

| Endpoint | Access |
|----------|--------|
| `/actuator/health`, `/actuator/info` | Public (load-balancer probes) |
| `GET /{application}/{profile}/**` | Permit-all (network-boundary protected — internal services only) |
| `GET /api/v1/flags/**` | Any authenticated service/user |
| `PUT /api/v1/flags/**` | `ROLE_ADMIN` or `ROLE_STAFF` only |
| All others | Authenticated |

---

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `PORT` | `8888` | HTTP port |
| `DB_URL` | `jdbc:postgresql://localhost:5432/slotconfig` | Postgres JDBC URL |
| `DB_USERNAME` | `slotconfig` | Postgres username |
| `DB_PASSWORD` | `slotconfig` | Postgres password |
| `AUTH_SERVICE_JWKS_URL` | `http://localhost:8081/.well-known/jwks.json` | JWKS endpoint for JWT validation |
| `CONFIG_GIT_URI` | `file:./config-repo` | Git repo URI (only used when active profile is `git`) |
| `CONFIG_GIT_BRANCH` | `main` | Git branch (only used with `git` profile) |
| `ENVIRONMENT` | `development` | Environment label in shared config |

---

## Running Locally

### Prerequisites
- Java 17+
- Docker (for Postgres) or a local Postgres instance

### With Docker Compose (recommended)
```bash
docker-compose up -d
```
This starts Postgres and the config service on port 8888.

### Without Docker (local Postgres required)
```bash
# Start Postgres first, then:
mvn spring-boot:run
```

### Test the Config Server
```bash
curl http://localhost:8888/game-engine-service/default | jq .
curl http://localhost:8888/jackpot-service/default | jq .
```

### Test the Feature Flag API
```bash
# Toggle gaffing (requires ADMIN JWT)
curl -X PUT http://localhost:8888/api/v1/flags/gaffing.enabled \
  -H "Authorization: ******" \
  -H "Content-Type: application/json" \
  -d '{"value": "true", "updatedBy": "admin@example.com"}'

# Read it back (any authenticated token)
curl http://localhost:8888/api/v1/flags/gaffing.enabled \
  -H "Authorization: ******"

# Check audit log
curl http://localhost:8888/api/v1/flags/gaffing.enabled/audit \
  -H "Authorization: ******"
```

---

## Running Tests
```bash
mvn test
```

Tests use H2 in-memory database (Flyway disabled in test profile, JPA creates schema via `create-drop`).

Test coverage:
- `SlotConfigServiceApplicationTests` — Spring context loads
- `ConfigServerIntegrationTest` — Config Server serves `game-engine-service` and `jackpot-service` configs
- `FeatureFlagServiceTest` — Unit tests: create flag, update flag, audit log correctness, not-found handling
- `FeatureFlagControllerTest` — Security slice: 401 unauthenticated, 403 wrong role, 200 with ADMIN role

---

## Building the Docker Image
```bash
docker build -t slot-config-service:latest .
```

Multi-stage build: Maven build in `maven:3.9-eclipse-temurin-17-alpine`, runtime in `eclipse-temurin:17-jre-alpine`. Runs as non-root `appuser`.

---

## Observability

- `GET /actuator/health` — liveness/readiness
- `GET /actuator/prometheus` — Micrometer metrics in Prometheus format
- `POST /actuator/refresh` — trigger config refresh (native: no-op; git-backed: re-reads from git)
- Structured JSON logging via `logstash-logback-encoder` — log lines are JSON objects with `@timestamp`, `level`, `logger_name`, `message`, etc.
