package com.transactguard.rule;

import com.transactguard.model.Alert;
import com.transactguard.model.RiskLevel;
import com.transactguard.model.Transaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RuleEngineTest {

    /**
     * A rule that always triggers — useful for testing the engine.
     */
    private static class AlwaysAlertRule implements TransactionRule {
        @Override
        public RuleName name() {
            return RuleName.HIGH_VALUE;
        }

        @Override
        public Optional<Alert> evaluate(Transaction transaction, List<Transaction> history) {
            return Optional.of(Alert.raise(
                    transaction, name(), RiskLevel.HIGH, "Test rule — always triggers"));
        }
    }

    /**
     * A rule that never triggers.
     */
    private static class NeverAlertRule implements TransactionRule {
        @Override
        public RuleName name() {
            return RuleName.VELOCITY;
        }

        @Override
        public Optional<Alert> evaluate(Transaction transaction, List<Transaction> history) {
            return Optional.empty();
        }
    }

    /**
     * A rule that throws an exception — verifies engine resilience.
     */
    private static class BrokenRule implements TransactionRule {
        @Override
        public RuleName name() {
            return RuleName.STRUCTURING;
        }

        @Override
        public Optional<Alert> evaluate(Transaction transaction, List<Transaction> history) {
            throw new RuntimeException("Simulated rule failure");
        }
    }

    private Transaction sampleTransaction() {
        return Transaction.of("ACC-001", "ACC-002", new BigDecimal("1000.00"), "EUR");
    }

    @Test
    void engineWithNoRulesReturnsNoAlerts() {
        RuleEngine engine = new RuleEngine(Collections.emptyList());
        List<Alert> alerts = engine.evaluate(sampleTransaction(), Collections.emptyList());
        assertTrue(alerts.isEmpty());
    }

    @Test
    void engineCollectsAlertsFromTriggeredRules() {
        RuleEngine engine = new RuleEngine(List.of(new AlwaysAlertRule()));
        List<Alert> alerts = engine.evaluate(sampleTransaction(), Collections.emptyList());

        assertEquals(1, alerts.size());
        assertEquals(RuleName.HIGH_VALUE, alerts.get(0).ruleName());
        assertEquals(RiskLevel.HIGH, alerts.get(0).riskLevel());
    }

    @Test
    void engineSkipsRulesThatDoNotTrigger() {
        RuleEngine engine = new RuleEngine(List.of(
                new NeverAlertRule(),
                new AlwaysAlertRule()));
        List<Alert> alerts = engine.evaluate(sampleTransaction(), Collections.emptyList());

        assertEquals(1, alerts.size());
        assertEquals(RuleName.HIGH_VALUE, alerts.get(0).ruleName());
    }

    @Test
    void engineIsolatesBrokenRulesFromHealthyOnes() {
        RuleEngine engine = new RuleEngine(List.of(
                new BrokenRule(),
                new AlwaysAlertRule()));
        List<Alert> alerts = engine.evaluate(sampleTransaction(), Collections.emptyList());

        // Broken rule should not prevent the healthy rule from running
        assertEquals(1, alerts.size());
        assertEquals(RuleName.HIGH_VALUE, alerts.get(0).ruleName());
    }

    @Test
    void registeredRulesReturnsAllRuleNames() {
        RuleEngine engine = new RuleEngine(List.of(
                new AlwaysAlertRule(),
                new NeverAlertRule()));
        List<RuleName> names = engine.registeredRules();

        assertEquals(2, names.size());
        assertTrue(names.contains(RuleName.HIGH_VALUE));
        assertTrue(names.contains(RuleName.VELOCITY));
    }

    @Test
    void alertsListIsImmutable() {
        RuleEngine engine = new RuleEngine(List.of(new AlwaysAlertRule()));
        List<Alert> alerts = engine.evaluate(sampleTransaction(), Collections.emptyList());

        assertThrows(UnsupportedOperationException.class,
                () -> alerts
                        .add(Alert.raise(sampleTransaction(), RuleName.ROUND_TRIP, RiskLevel.LOW, "should fail")));
    }
}
