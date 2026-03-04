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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class HighValueRuleTest {

    private final HighValueRule rule = new HighValueRule(new BigDecimal("10000"));

    @Test
    @DisplayName("Should trigger for amount above threshold")
    void triggersAboveThreshold() {
        Transaction txn = Transaction.of("ACC-001", "ACC-002", new BigDecimal("15000"), "EUR");
        Optional<Alert> alert = rule.evaluate(txn, List.of());

        assertTrue(alert.isPresent());
        assertEquals(RuleName.HIGH_VALUE, alert.get().ruleName());
        assertEquals(RiskLevel.MEDIUM, alert.get().riskLevel());
    }

    @Test
    @DisplayName("Should not trigger for amount at threshold")
    void doesNotTriggerAtThreshold() {
        Transaction txn = Transaction.of("ACC-001", "ACC-002", new BigDecimal("10000"), "EUR");
        Optional<Alert> alert = rule.evaluate(txn, List.of());

        assertTrue(alert.isEmpty());
    }

    @Test
    @DisplayName("Should not trigger for amount below threshold")
    void doesNotTriggerBelowThreshold() {
        Transaction txn = Transaction.of("ACC-001", "ACC-002", new BigDecimal("5000"), "EUR");
        Optional<Alert> alert = rule.evaluate(txn, List.of());

        assertTrue(alert.isEmpty());
    }

    @Nested
    @DisplayName("Risk level scaling")
    class RiskLevelTests {

        @Test
        @DisplayName("MEDIUM for amount just above threshold")
        void mediumRisk() {
            Transaction txn = Transaction.of("ACC-001", "ACC-002", new BigDecimal("15000"), "EUR");
            Optional<Alert> alert = rule.evaluate(txn, List.of());

            assertTrue(alert.isPresent());
            assertEquals(RiskLevel.MEDIUM, alert.get().riskLevel());
        }

        @Test
        @DisplayName("HIGH for amount above 5x threshold")
        void highRisk() {
            Transaction txn = Transaction.of("ACC-001", "ACC-002", new BigDecimal("60000"), "EUR");
            Optional<Alert> alert = rule.evaluate(txn, List.of());

            assertTrue(alert.isPresent());
            assertEquals(RiskLevel.HIGH, alert.get().riskLevel());
        }

        @Test
        @DisplayName("CRITICAL for amount above 10x threshold")
        void criticalRisk() {
            Transaction txn = Transaction.of("ACC-001", "ACC-002", new BigDecimal("150000"), "EUR");
            Optional<Alert> alert = rule.evaluate(txn, List.of());

            assertTrue(alert.isPresent());
            assertEquals(RiskLevel.CRITICAL, alert.get().riskLevel());
        }
    }

    @Test
    @DisplayName("Alert description includes amount and currency")
    void alertDescriptionContainsDetails() {
        Transaction txn = Transaction.of("ACC-001", "ACC-002", new BigDecimal("25000"), "EUR");
        Optional<Alert> alert = rule.evaluate(txn, List.of());

        assertTrue(alert.isPresent());
        assertTrue(alert.get().description().contains("25000"));
        assertTrue(alert.get().description().contains("EUR"));
    }
}
