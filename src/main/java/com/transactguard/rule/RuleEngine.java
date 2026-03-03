package com.transactguard.rule;

import com.transactguard.model.Alert;
import com.transactguard.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Orchestrates all registered {@link TransactionRule} implementations.
 *
 * <p>
 * Spring auto-injects every {@code @Component} that implements
 * {@link TransactionRule}, so adding a new rule requires zero changes here.
 * </p>
 */
@Component
public class RuleEngine {

    private static final Logger log = LoggerFactory.getLogger(RuleEngine.class);

    private final List<TransactionRule> rules;

    public RuleEngine(List<TransactionRule> rules) {
        this.rules = rules;
        log.info("RuleEngine initialized with {} rules: {}",
                rules.size(),
                rules.stream().map(TransactionRule::name).toList());
    }

    /**
     * Run all rules against a transaction and collect any alerts.
     *
     * @param transaction the incoming transaction
     * @param history     recent transactions for the same account
     * @return list of alerts (empty if no rules triggered)
     */
    public List<Alert> evaluate(Transaction transaction, List<Transaction> history) {
        List<Alert> alerts = new ArrayList<>();

        for (TransactionRule rule : rules) {
            try {
                Optional<Alert> alert = rule.evaluate(transaction, history);
                alert.ifPresent(a -> {
                    log.warn("Rule [{}] triggered for transaction {} — risk: {}",
                            rule.name(), transaction.id(), a.riskLevel());
                    alerts.add(a);
                });
            } catch (Exception e) {
                log.error("Rule [{}] threw exception for transaction {}: {}",
                        rule.name(), transaction.id(), e.getMessage(), e);
            }
        }

        return Collections.unmodifiableList(alerts);
    }

    /**
     * @return names of all registered rules
     */
    public List<String> registeredRules() {
        return rules.stream().map(TransactionRule::name).toList();
    }
}
