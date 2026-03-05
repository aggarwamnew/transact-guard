package com.transactguard.rule;

import com.transactguard.model.Alert;
import com.transactguard.model.RiskLevel;
import com.transactguard.model.Transaction;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class VelocityRuleTest {

    // Low threshold for easy testing: max 5 transactions in 1 hour
    private final VelocityRule rule = new VelocityRule(5, Duration.ofHours(1));

    private Transaction txnAt(Instant timestamp) {
        return new Transaction(
                "txn-" + System.nanoTime(),
                "ACC-001", "ACC-002",
                new BigDecimal("500"), "EUR",
                timestamp);
    }

    @Test
    @DisplayName("Should trigger when transaction count exceeds threshold within window")
    void triggersWhenVelocityExceeded() {
        Instant now = Instant.now();
        Transaction current = txnAt(now);

        List<Transaction> history = List.of(
                txnAt(now.minus(Duration.ofMinutes(5))),
                txnAt(now.minus(Duration.ofMinutes(10))),
                txnAt(now.minus(Duration.ofMinutes(15))),
                txnAt(now.minus(Duration.ofMinutes(20))),
                txnAt(now.minus(Duration.ofMinutes(25))));

        // 5 history + 1 current = 6, exceeds limit of 5
        Optional<Alert> alert = rule.evaluate(current, history);

        assertTrue(alert.isPresent());
        assertEquals(RuleName.VELOCITY, alert.get().ruleName());
        assertEquals(RiskLevel.MEDIUM, alert.get().riskLevel());
    }

    @Test
    @DisplayName("Should not trigger when count is exactly at threshold")
    void doesNotTriggerAtThreshold() {
        Instant now = Instant.now();
        Transaction current = txnAt(now);

        List<Transaction> history = List.of(
                txnAt(now.minus(Duration.ofMinutes(5))),
                txnAt(now.minus(Duration.ofMinutes(10))),
                txnAt(now.minus(Duration.ofMinutes(15))),
                txnAt(now.minus(Duration.ofMinutes(20))));

        // 4 history + 1 current = 5, at limit (not over)
        Optional<Alert> alert = rule.evaluate(current, history);

        assertTrue(alert.isEmpty());
    }

    @Test
    @DisplayName("Should not trigger when count is below threshold")
    void doesNotTriggerBelowThreshold() {
        Instant now = Instant.now();
        Transaction current = txnAt(now);

        List<Transaction> history = List.of(
                txnAt(now.minus(Duration.ofMinutes(5))),
                txnAt(now.minus(Duration.ofMinutes(10))));

        // 2 history + 1 current = 3, well under limit
        Optional<Alert> alert = rule.evaluate(current, history);

        assertTrue(alert.isEmpty());
    }

    @Test
    @DisplayName("Should not trigger when transactions are outside time window")
    void doesNotTriggerOutsideWindow() {
        Instant now = Instant.now();
        Transaction current = txnAt(now);

        List<Transaction> history = List.of(
                txnAt(now.minus(Duration.ofHours(2))),
                txnAt(now.minus(Duration.ofHours(3))),
                txnAt(now.minus(Duration.ofHours(4))),
                txnAt(now.minus(Duration.ofHours(5))),
                txnAt(now.minus(Duration.ofHours(6))));

        // All 5 history outside the 1-hour window
        Optional<Alert> alert = rule.evaluate(current, history);

        assertTrue(alert.isEmpty());
    }

    @Test
    @DisplayName("Should not trigger with empty history")
    void doesNotTriggerWithEmptyHistory() {
        Transaction current = txnAt(Instant.now());

        Optional<Alert> alert = rule.evaluate(current, List.of());

        assertTrue(alert.isEmpty());
    }

    @Nested
    @DisplayName("Risk level scaling")
    class RiskLevelTests {

        @Test
        @DisplayName("MEDIUM when just over threshold")
        void mediumRisk() {
            Instant now = Instant.now();
            Transaction current = txnAt(now);

            // 5 history + 1 current = 6 (just over limit of 5)
            List<Transaction> history = createHistory(5, now);

            Optional<Alert> alert = rule.evaluate(current, history);

            assertTrue(alert.isPresent());
            assertEquals(RiskLevel.MEDIUM, alert.get().riskLevel());
        }

        @Test
        @DisplayName("HIGH when at 2x threshold")
        void highRisk() {
            Instant now = Instant.now();
            Transaction current = txnAt(now);

            // 9 history + 1 current = 10 (2x limit of 5)
            List<Transaction> history = createHistory(9, now);

            Optional<Alert> alert = rule.evaluate(current, history);

            assertTrue(alert.isPresent());
            assertEquals(RiskLevel.HIGH, alert.get().riskLevel());
        }

        @Test
        @DisplayName("CRITICAL when at 3x threshold")
        void criticalRisk() {
            Instant now = Instant.now();
            Transaction current = txnAt(now);

            // 14 history + 1 current = 15 (3x limit of 5)
            List<Transaction> history = createHistory(14, now);

            Optional<Alert> alert = rule.evaluate(current, history);

            assertTrue(alert.isPresent());
            assertEquals(RiskLevel.CRITICAL, alert.get().riskLevel());
        }

        private List<Transaction> createHistory(int count, Instant now) {
            List<Transaction> history = new ArrayList<>();
            for (int i = 1; i <= count; i++) {
                history.add(txnAt(now.minus(Duration.ofMinutes(i))));
            }
            return history;
        }
    }

    @Test
    @DisplayName("Alert description includes count and window details")
    void alertDescriptionContainsDetails() {
        Instant now = Instant.now();
        Transaction current = txnAt(now);

        List<Transaction> history = List.of(
                txnAt(now.minus(Duration.ofMinutes(5))),
                txnAt(now.minus(Duration.ofMinutes(10))),
                txnAt(now.minus(Duration.ofMinutes(15))),
                txnAt(now.minus(Duration.ofMinutes(20))),
                txnAt(now.minus(Duration.ofMinutes(25))));

        Optional<Alert> alert = rule.evaluate(current, history);

        assertTrue(alert.isPresent());
        assertTrue(alert.get().description().contains("6 transactions"));
        assertTrue(alert.get().description().contains("ACC-001"));
        assertTrue(alert.get().description().contains("60-minute"));
    }
}
