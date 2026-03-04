package com.transactguard.rule;

import com.transactguard.model.Alert;
import com.transactguard.model.RiskLevel;
import com.transactguard.model.Transaction;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class StructuringRuleTest {

    private final StructuringRule rule = new StructuringRule(
            new BigDecimal("10000"),
            Duration.ofHours(24),
            3);

    private Transaction txnAt(String amount, Instant timestamp) {
        return new Transaction(
                "txn-" + System.nanoTime(),
                "ACC-001", "ACC-002",
                new BigDecimal(amount), "EUR",
                timestamp);
    }

    @Test
    @DisplayName("Should trigger when multiple small transactions exceed threshold within window")
    void triggersOnStructuringPattern() {
        Instant now = Instant.now();
        Transaction current = txnAt("4000", now);

        List<Transaction> history = List.of(
                txnAt("3500", now.minus(Duration.ofHours(2))),
                txnAt("3500", now.minus(Duration.ofHours(4))));

        Optional<Alert> alert = rule.evaluate(current, history);

        assertTrue(alert.isPresent());
        assertEquals(RuleName.STRUCTURING, alert.get().ruleName());
        assertEquals(RiskLevel.HIGH, alert.get().riskLevel());
    }

    @Test
    @DisplayName("Should not trigger when total is below threshold")
    void doesNotTriggerBelowThreshold() {
        Instant now = Instant.now();
        Transaction current = txnAt("2000", now);

        List<Transaction> history = List.of(
                txnAt("2000", now.minus(Duration.ofHours(2))),
                txnAt("2000", now.minus(Duration.ofHours(4))));

        Optional<Alert> alert = rule.evaluate(current, history);

        assertTrue(alert.isEmpty());
    }

    @Test
    @DisplayName("Should not trigger when too few transactions")
    void doesNotTriggerWithFewTransactions() {
        Instant now = Instant.now();
        Transaction current = txnAt("6000", now);

        List<Transaction> history = List.of(
                txnAt("6000", now.minus(Duration.ofHours(2))));

        // Only 2 transactions (current + 1 history), minimum is 3
        Optional<Alert> alert = rule.evaluate(current, history);

        assertTrue(alert.isEmpty());
    }

    @Test
    @DisplayName("Should not trigger when transactions are outside time window")
    void doesNotTriggerOutsideWindow() {
        Instant now = Instant.now();
        Transaction current = txnAt("4000", now);

        List<Transaction> history = List.of(
                txnAt("4000", now.minus(Duration.ofHours(25))),
                txnAt("4000", now.minus(Duration.ofHours(30))));

        Optional<Alert> alert = rule.evaluate(current, history);

        assertTrue(alert.isEmpty());
    }

    @Test
    @DisplayName("Should not trigger when current transaction is above threshold (HighValueRule handles this)")
    void doesNotTriggerForHighValueTransaction() {
        Instant now = Instant.now();
        Transaction current = txnAt("15000", now);

        List<Transaction> history = List.of(
                txnAt("3000", now.minus(Duration.ofHours(2))),
                txnAt("3000", now.minus(Duration.ofHours(4))));

        Optional<Alert> alert = rule.evaluate(current, history);

        assertTrue(alert.isEmpty());
    }

    @Test
    @DisplayName("Alert description includes transaction count and total amount")
    void alertDescriptionContainsDetails() {
        Instant now = Instant.now();
        Transaction current = txnAt("4000", now);

        List<Transaction> history = List.of(
                txnAt("3500", now.minus(Duration.ofHours(2))),
                txnAt("3500", now.minus(Duration.ofHours(4))));

        Optional<Alert> alert = rule.evaluate(current, history);

        assertTrue(alert.isPresent());
        assertTrue(alert.get().description().contains("3 transactions"));
        assertTrue(alert.get().description().contains("11000"));
    }
}
