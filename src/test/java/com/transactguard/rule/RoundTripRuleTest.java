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

class RoundTripRuleTest {

    private final RoundTripRule rule = new RoundTripRule(
            Duration.ofHours(24),
            5);

    private Transaction txn(String from, String to, Instant timestamp) {
        return new Transaction(
                "txn-" + System.nanoTime(),
                from, to,
                new BigDecimal("1000"), "EUR",
                timestamp);
    }

    @Test
    @DisplayName("Should detect simple A → B → A round-trip")
    void detectsSimpleRoundTrip() {
        Instant now = Instant.now();

        // Current: A → B
        Transaction current = txn("ACC-A", "ACC-B", now);

        // History: B → A (completes the circle)
        List<Transaction> history = List.of(
                txn("ACC-B", "ACC-A", now.minus(Duration.ofHours(2))));

        Optional<Alert> alert = rule.evaluate(current, history);

        assertTrue(alert.isPresent());
        assertEquals(RuleName.ROUND_TRIP, alert.get().ruleName());
        assertEquals(RiskLevel.HIGH, alert.get().riskLevel());
    }

    @Test
    @DisplayName("Should detect multi-hop chain A → B → C → A")
    void detectsMultiHopRoundTrip() {
        Instant now = Instant.now();

        // Current: A → B
        Transaction current = txn("ACC-A", "ACC-B", now);

        // History: B → C, then C → A (three-hop circle)
        List<Transaction> history = List.of(
                txn("ACC-B", "ACC-C", now.minus(Duration.ofHours(2))),
                txn("ACC-C", "ACC-A", now.minus(Duration.ofHours(1))));

        Optional<Alert> alert = rule.evaluate(current, history);

        assertTrue(alert.isPresent());
        assertTrue(alert.get().description().contains("ACC-A"));
        assertTrue(alert.get().description().contains("ACC-B"));
    }

    @Test
    @DisplayName("Should detect longer chain A → B → C → D → A")
    void detectsFourHopRoundTrip() {
        Instant now = Instant.now();

        // Current: A → B
        Transaction current = txn("ACC-A", "ACC-B", now);

        // History: B → C → D → A
        List<Transaction> history = List.of(
                txn("ACC-B", "ACC-C", now.minus(Duration.ofHours(3))),
                txn("ACC-C", "ACC-D", now.minus(Duration.ofHours(2))),
                txn("ACC-D", "ACC-A", now.minus(Duration.ofHours(1))));

        Optional<Alert> alert = rule.evaluate(current, history);

        assertTrue(alert.isPresent());
    }

    @Test
    @DisplayName("Should not trigger for normal one-way transfers")
    void doesNotTriggerForOneWayTransfer() {
        Instant now = Instant.now();

        // Current: A → B
        Transaction current = txn("ACC-A", "ACC-B", now);

        // History: unrelated transfers, no path back to A
        List<Transaction> history = List.of(
                txn("ACC-B", "ACC-C", now.minus(Duration.ofHours(2))),
                txn("ACC-C", "ACC-D", now.minus(Duration.ofHours(1))));

        Optional<Alert> alert = rule.evaluate(current, history);

        assertTrue(alert.isEmpty());
    }

    @Test
    @DisplayName("Should not trigger when chain is outside time window")
    void doesNotTriggerOutsideWindow() {
        Instant now = Instant.now();

        // Current: A → B
        Transaction current = txn("ACC-A", "ACC-B", now);

        // History: B → A, but 25 hours ago (outside 24h window)
        List<Transaction> history = List.of(
                txn("ACC-B", "ACC-A", now.minus(Duration.ofHours(25))));

        Optional<Alert> alert = rule.evaluate(current, history);

        assertTrue(alert.isEmpty());
    }

    @Test
    @DisplayName("Should respect max chain depth limit")
    void respectsMaxChainDepth() {
        // Rule with max depth of 2 hops
        RoundTripRule shallowRule = new RoundTripRule(Duration.ofHours(24), 2);
        Instant now = Instant.now();

        // Current: A → B
        Transaction current = txn("ACC-A", "ACC-B", now);

        // History: B → C → D → A (3 hops, exceeds depth limit of 2)
        List<Transaction> history = List.of(
                txn("ACC-B", "ACC-C", now.minus(Duration.ofHours(3))),
                txn("ACC-C", "ACC-D", now.minus(Duration.ofHours(2))),
                txn("ACC-D", "ACC-A", now.minus(Duration.ofHours(1))));

        Optional<Alert> alert = shallowRule.evaluate(current, history);

        assertTrue(alert.isEmpty());
    }

    @Test
    @DisplayName("Should not trigger with empty history")
    void doesNotTriggerWithEmptyHistory() {
        Transaction current = txn("ACC-A", "ACC-B", Instant.now());

        Optional<Alert> alert = rule.evaluate(current, List.of());

        assertTrue(alert.isEmpty());
    }

    @Test
    @DisplayName("Alert description includes chain details")
    void alertDescriptionContainsChainDetails() {
        Instant now = Instant.now();

        Transaction current = txn("ACC-A", "ACC-B", now);

        List<Transaction> history = List.of(
                txn("ACC-B", "ACC-A", now.minus(Duration.ofHours(2))));

        Optional<Alert> alert = rule.evaluate(current, history);

        assertTrue(alert.isPresent());
        String desc = alert.get().description();
        assertTrue(desc.contains("Circular money flow"));
        assertTrue(desc.contains("ACC-A"));
        assertTrue(desc.contains("ACC-B"));
        assertTrue(desc.contains("24h"));
    }

    @Test
    @DisplayName("Should not trigger for self-transfers")
    void doesNotTriggerForSelfTransfer() {
        Instant now = Instant.now();

        // Self-transfer: A → A
        Transaction current = txn("ACC-A", "ACC-A", now);

        List<Transaction> history = List.of(
                txn("ACC-A", "ACC-A", now.minus(Duration.ofHours(1))));

        Optional<Alert> alert = rule.evaluate(current, history);

        assertTrue(alert.isEmpty());
    }
}
