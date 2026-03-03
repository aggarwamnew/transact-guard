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
- **Structuring** — Multiple transactions just below reporting thresholds (e.g., 5 × €9,900 instead of 1 × €49,500)
- **Velocity** — Unusual frequency of transactions from the same account in a short window
- **Round-tripping** — Circular money flow: A → B → C → A
- **Anomaly** — Transaction amount deviates significantly from the account's historical pattern

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
- [ ] **Day 3** — Implement all 4 rules: Structuring, Velocity, Round-tripping, Anomaly
- [ ] **Day 4** — `POST /transactions` endpoint + unit tests (in-memory storage)
- [ ] **Day 5** — `GET /alerts` endpoint + unit tests (in-memory storage)
- [ ] **Day 6** — AWS DynamoDB integration (replace in-memory store)
- [ ] **Day 7** — Release: full integration test, documentation polish, GitHub publish

---

## License
Apache-2.0
