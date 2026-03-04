package com.transactguard.rule;

import com.transactguard.model.Alert;
import com.transactguard.model.RiskLevel;
import com.transactguard.model.Transaction;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Detects structuring (also known as "smurfing") patterns.
 *
 * <p>
 * Structuring is the practice of splitting a large transaction into
 * multiple smaller ones to avoid triggering reporting thresholds.
 * This is a criminal offence under AML regulations in most jurisdictions.
 * </p>
 *
 * <p>
 * Detection logic: if multiple transactions from the same account within
 * a time window sum to more than the reporting threshold, but each
 * individual transaction is below that threshold, the pattern is flagged.
 * </p>
 */
@Component
public class StructuringRule implements TransactionRule {

    private static final BigDecimal REPORTING_THRESHOLD = new BigDecimal("10000");
    private static final Duration TIME_WINDOW = Duration.ofHours(24);
    private static final int MIN_TRANSACTION_COUNT = 3;

    private final BigDecimal reportingThreshold;
    private final Duration timeWindow;
    private final int minTransactionCount;

    public StructuringRule() {
        this(REPORTING_THRESHOLD, TIME_WINDOW, MIN_TRANSACTION_COUNT);
    }

    /** Visible for testing — allows injecting custom parameters. */
    StructuringRule(BigDecimal reportingThreshold, Duration timeWindow, int minTransactionCount) {
        this.reportingThreshold = reportingThreshold;
        this.timeWindow = timeWindow;
        this.minTransactionCount = minTransactionCount;
    }

    @Override
    public RuleName name() {
        return RuleName.STRUCTURING;
    }

    @Override
    public Optional<Alert> evaluate(Transaction transaction, List<Transaction> history) {
        // Guard: if the current transaction is above threshold, HighValueRule handles
        // it
        if (transaction.amount().compareTo(reportingThreshold) >= 0) {
            return Optional.empty();
        }

        Instant cutoff = transaction.timestamp().minus(timeWindow);

        // Recent history within the time window, each below threshold
        List<Transaction> recentTransactions = history.stream()
                .filter(t -> t.timestamp().isAfter(cutoff))
                .filter(t -> t.amount().compareTo(reportingThreshold) < 0)
                .toList();

        // Count includes the current transaction
        int totalCount = recentTransactions.size() + 1;
        BigDecimal totalAmount = recentTransactions.stream()
                .map(Transaction::amount)
                .reduce(transaction.amount(), BigDecimal::add);

        if (totalCount >= minTransactionCount && totalAmount.compareTo(reportingThreshold) > 0) {
            return Optional.of(Alert.raise(
                    transaction,
                    name(),
                    RiskLevel.HIGH,
                    String.format(
                            "Suspected structuring: %d transactions totalling %s %s within %dh window (threshold: %s)",
                            totalCount, totalAmount, transaction.currency(),
                            timeWindow.toHours(), reportingThreshold)));
        }

        return Optional.empty();
    }
}
