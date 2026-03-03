package com.transactguard.rule;

import com.transactguard.model.Alert;
import com.transactguard.model.Transaction;

import java.util.List;
import java.util.Optional;

/**
 * Core interface for AML transaction rules.
 *
 * <p>
 * Each rule inspects a transaction (with access to historical context)
 * and returns an {@link Alert} if suspicious activity is detected.
 * </p>
 *
 * <p>
 * Adding a new detection pattern requires:
 * 1. Adding the rule name to {@link RuleName} (audit-controlled enum)
 * 2. Implementing this interface and annotating with {@code @Component}
 * Spring auto-discovery handles the rest — no engine changes required.
 * </p>
 */
public interface TransactionRule {

    /**
     * The registered name of this rule from the {@link RuleName} enum.
     * All rule names must be pre-approved and registered for compliance.
     */
    RuleName name();

    /**
     * Evaluate a transaction against this rule.
     *
     * @param transaction the incoming transaction to evaluate
     * @param history     recent transactions for the same account (for pattern
     *                    detection)
     * @return an alert if the rule is triggered, or empty if the transaction is
     *         clean
     */
    Optional<Alert> evaluate(Transaction transaction, List<Transaction> history);
}
