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
 *
 * <p>
 * All evaluations are logged for audit trail compliance.
 * </p>
 */
@Component
public class RuleEngine {

    private static final Logger log = LoggerFactory.getLogger(RuleEngine.class);

    private final List<TransactionRule> rules;

    public RuleEngine(List<TransactionRule> rules) {
        this.rules = rules;
        log.info("AUDIT | RuleEngine initialized | ruleCount={} | rules={}",
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
        log.info("AUDIT | Evaluation started | txId={} | from={} | to={} | amount={} {} | historySize={}",
                transaction.id(), transaction.fromAccount(), transaction.toAccount(),
                transaction.amount(), transaction.currency(), history.size());

        List<Alert> alerts = new ArrayList<>();

        for (TransactionRule rule : rules) {
            try {
                Optional<Alert> alert = rule.evaluate(transaction, history);
                if (alert.isPresent()) {
                    alerts.add(alert.get());
                    log.warn("AUDIT | Rule TRIGGERED | txId={} | rule={} | risk={} | alertId={} | reason={}",
                            transaction.id(), rule.name(), alert.get().riskLevel(),
                            alert.get().id(), alert.get().description());
                } else {
                    log.debug("AUDIT | Rule passed | txId={} | rule={}",
                            transaction.id(), rule.name());
                }
            } catch (Exception e) {
                log.error("AUDIT | Rule EXCEPTION | txId={} | rule={} | error={}",
                        rule.name(), transaction.id(), e.getMessage(), e);
            }
        }

        log.info("AUDIT | Evaluation complete | txId={} | rulesEvaluated={} | alertsRaised={}",
                transaction.id(), rules.size(), alerts.size());

        return Collections.unmodifiableList(alerts);
    }

    /**
     * @return names of all registered rules
     */
    public List<RuleName> registeredRules() {
        return rules.stream().map(TransactionRule::name).toList();
    }
}
