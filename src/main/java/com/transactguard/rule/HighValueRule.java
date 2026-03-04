package com.transactguard.rule;

import com.transactguard.model.Alert;
import com.transactguard.model.RiskLevel;
import com.transactguard.model.Transaction;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Flags transactions above a configurable threshold.
 *
 * <p>
 * In most jurisdictions, financial institutions must report transactions
 * exceeding a certain value (e.g., €10,000 in the EU under AMLD).
 * This rule catches single high-value transfers that require scrutiny.
 * </p>
 */
@Component
public class HighValueRule implements TransactionRule {

    private static final BigDecimal DEFAULT_THRESHOLD = new BigDecimal("10000");

    private final BigDecimal threshold;

    public HighValueRule() {
        this(DEFAULT_THRESHOLD);
    }

    /** Visible for testing — allows injecting a custom threshold. */
    HighValueRule(BigDecimal threshold) {
        this.threshold = threshold;
    }

    @Override
    public RuleName name() {
        return RuleName.HIGH_VALUE;
    }

    @Override
    public Optional<Alert> evaluate(Transaction transaction, List<Transaction> history) {
        if (transaction.amount().compareTo(threshold) > 0) {
            return Optional.of(Alert.raise(
                    transaction,
                    name(),
                    determineRisk(transaction.amount()),
                    String.format("Transaction amount %s %s exceeds threshold %s",
                            transaction.amount(), transaction.currency(), threshold)));
        }
        return Optional.empty();
    }

    private RiskLevel determineRisk(BigDecimal amount) {
        if (amount.compareTo(threshold.multiply(BigDecimal.TEN)) > 0) {
            return RiskLevel.CRITICAL;
        }
        if (amount.compareTo(threshold.multiply(BigDecimal.valueOf(5))) > 0) {
            return RiskLevel.HIGH;
        }
        return RiskLevel.MEDIUM;
    }
}
