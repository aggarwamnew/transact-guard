package com.transactguard.rule;

import com.transactguard.model.Alert;
import com.transactguard.model.RiskLevel;
import com.transactguard.model.Transaction;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Detects unusually high transaction frequency from a single account.
 *
 * <p>
 * A burst of outbound transactions in a short window is a strong indicator
 * of "money mule" activity — accounts used as intermediaries to layer
 * illicit funds through the financial system quickly.
 * </p>
 *
 * <p>
 * Detection logic: count all outbound transactions from the same account
 * within a configurable time window. If the count exceeds a threshold,
 * an alert is raised with risk scaling based on how far over the limit.
 * </p>
 */
@Component
public class VelocityRule implements TransactionRule {

    private static final int DEFAULT_MAX_TRANSACTIONS = 10;
    private static final Duration DEFAULT_TIME_WINDOW = Duration.ofHours(1);

    private final int maxTransactions;
    private final Duration timeWindow;

    public VelocityRule() {
        this(DEFAULT_MAX_TRANSACTIONS, DEFAULT_TIME_WINDOW);
    }

    /** Visible for testing — allows injecting custom parameters. */
    VelocityRule(int maxTransactions, Duration timeWindow) {
        this.maxTransactions = maxTransactions;
        this.timeWindow = timeWindow;
    }

    @Override
    public RuleName name() {
        return RuleName.VELOCITY;
    }

    @Override
    public Optional<Alert> evaluate(Transaction transaction, List<Transaction> history) {
        Instant cutoff = transaction.timestamp().minus(timeWindow);

        long recentCount = history.stream()
                .filter(t -> t.timestamp().isAfter(cutoff))
                .count();

        // Include the current transaction in the total
        long totalCount = recentCount + 1;

        if (totalCount > maxTransactions) {
            return Optional.of(Alert.raise(
                    transaction,
                    name(),
                    determineRisk(totalCount),
                    String.format(
                            "Velocity breach: %d transactions from account %s within %d-minute window (limit: %d)",
                            totalCount, transaction.fromAccount(),
                            timeWindow.toMinutes(), maxTransactions)));
        }

        return Optional.empty();
    }

    private RiskLevel determineRisk(long count) {
        if (count >= maxTransactions * 3L) {
            return RiskLevel.CRITICAL;
        }
        if (count >= maxTransactions * 2L) {
            return RiskLevel.HIGH;
        }
        return RiskLevel.MEDIUM;
    }
}
