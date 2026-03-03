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
 * Adding a new detection pattern is as simple as implementing this
 * interface and annotating the class with {@code @Component}. Spring
 * auto-discovery handles the rest — no core code changes required.
 * </p>
 */
public interface TransactionRule {

    /**
     * Unique name of this rule (e.g., "STRUCTURING", "VELOCITY").
     */
    String name();

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
