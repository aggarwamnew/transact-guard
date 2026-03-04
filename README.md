# TransactGuard

A Spring Boot AML (Anti-Money Laundering) transaction monitoring engine. Flags suspicious financial activity using pluggable, rule-based detection.

## Architecture

```
POST /transactions → Ingest → Rule Engine → Flag → Store
GET  /alerts       → Query flagged transactions with risk scores
```

### Rule Engine (Pluggable)
Each detection rule implements the `TransactionRule` interface. Adding a new rule = adding one class.

**Included rules:**
- **High Value** — Single transactions above reporting threshold (€10,000 EU AMLD), risk scales with amount
- **Structuring** — Multiple transactions just below reporting thresholds within a time window (smurfing detection)
- **Velocity** — Unusual frequency of transactions from the same account in a short window
- **Round-tripping** — Circular money flow: A → B → C → A

## Tech Stack
- Java 17+
- Spring Boot 3.x
- REST API (Spring Web)
- AWS DynamoDB (persistence)
- JUnit 5 + MockMvc (testing)

---

## 7-Day Build Plan

- [x] **Day 1** — Project scaffolding, README, CI skeleton
- [x] **Day 2** — `TransactionRule` interface + rule engine core
- [x] **Day 3** — `RuleName` enum (closed set for audit compliance) + structured AUDIT logging
- [x] **Day 4** — `HighValueRule` + `StructuringRule` with full test suites (20 tests passing)
- [ ] **Day 5** — `VelocityRule` + `POST /transactions` REST endpoint (in-memory storage)
- [ ] **Day 6** — `GET /alerts` endpoint + in-memory alert store + integration tests
- [ ] **Day 7** — AWS DynamoDB integration, documentation polish, release

---

## License
Apache-2.0
