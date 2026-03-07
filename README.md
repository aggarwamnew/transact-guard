# TransactGuard

A Spring Boot AML (Anti-Money Laundering) transaction monitoring engine. Flags suspicious financial activity using pluggable, rule-based detection.

**60 tests** | **4 detection rules** | **REST API** | **Audit logging**

## Architecture

```
POST /api/transactions → Ingest → Rule Engine → Flag → Store → Response
GET  /api/alerts        → Query flagged transactions with risk scores + filters
GET  /api/alerts?riskLevel=HIGH&status=PENDING_REVIEW
```

### Rule Engine (Pluggable)
Each detection rule implements the `TransactionRule` interface. Adding a new rule = adding one class.

**Included rules:**
- **High Value** — Single transactions above reporting threshold (€10,000 EU AMLD), risk scales with amount
- **Structuring** — Multiple transactions just below reporting thresholds within a time window (smurfing detection)
- **Velocity** — Unusual frequency of transactions from the same account in a short window
- **Round-tripping** — Circular money flow: A → B → C → A

### API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/transactions` | Ingest transaction, run all rules, return alerts |
| `GET` | `/api/transactions/{id}` | Look up stored transaction |
| `GET` | `/api/alerts` | Query alerts (optional `riskLevel`, `status` filters) |
| `GET` | `/api/alerts/{id}` | Look up specific alert |

Request validation via Bean Validation (`@NotBlank`, `@Positive`). Invalid requests return `400`.

## Tech Stack
- Java 17+
- Spring Boot 3.x
- REST API (Spring Web + Validation)
- In-memory stores (ConcurrentHashMap with account indexing)
- JUnit 5 + MockMvc (60 tests)
- Structured AUDIT logging (SLF4J)

## Running

```bash
# Build and test
mvn clean test

# Run server
mvn spring-boot:run

# Test it
curl -X POST http://localhost:8080/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"fromAccount":"ACC-001","toAccount":"ACC-002","amount":15000,"currency":"EUR"}'
```

---

## Future Roadmap

The core engine is complete. The following extensions would move this toward production-grade infrastructure:

### AWS DynamoDB Integration
Replace in-memory `TransactionStore` and `AlertStore` with DynamoDB tables. The store interface is already abstracted — swap implementations without changing service/controller layer. Would add TTL-based expiry for transaction history and GSI (Global Secondary Index) for querying alerts by account, risk level, or date range.

### Kafka Event Streaming
Ingest transactions from a Kafka topic instead of (or in addition to) the REST endpoint. Publish alerts to a separate topic for downstream consumers (case management, notification services). Enables real-time, high-throughput processing at scale.

### Dashboard & Metrics
`GET /api/metrics` endpoint exposing operational stats: transactions processed, alerts raised by rule, average risk scores, rule execution times. Could feed into Grafana or a lightweight React dashboard for compliance officers.

### Additional Rules
- **Geographic anomaly** — flag transactions from unusual jurisdictions
- **Time pattern** — flag activity outside business hours
- **Counterparty network** — graph-based detection of layering across multiple accounts

---

## Build History

| Day | Milestone | Tests |
|-----|-----------|-------|
| 1 | Project scaffolding, README | — |
| 2 | `TransactionRule` interface + rule engine core | — |
| 3 | `RuleName` enum + structured AUDIT logging | — |
| 4 | `HighValueRule` + `StructuringRule` | 20 |
| 5 | `VelocityRule` + `RoundTripRule` | 38 |
| 6 | `POST /transactions` + in-memory stores | 60 |
| 7 | `GET /alerts` + MockMvc integration tests | 60 |
