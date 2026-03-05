package com.transactguard.rule;

import com.transactguard.model.Alert;
import com.transactguard.model.RiskLevel;
import com.transactguard.model.Transaction;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Detects circular money flows (round-tripping / layering).
 *
 * <p>
 * Round-tripping occurs when funds move through a chain of accounts
 * and return to the originator: A → B → C → A. This is a hallmark
 * of money laundering "layering" — the second stage where criminals
 * obscure the trail by moving money through multiple intermediaries.
 * </p>
 *
 * <p>
 * Detection logic: starting from the current transaction's destination account,
 * perform a breadth-first search through recent transaction history looking for
 * a chain that leads back to the originating account. The search is bounded by
 * a configurable time window and maximum chain depth to prevent explosion.
 * </p>
 */
@Component
public class RoundTripRule implements TransactionRule {

    private static final Duration DEFAULT_TIME_WINDOW = Duration.ofHours(24);
    private static final int DEFAULT_MAX_CHAIN_DEPTH = 5;

    private final Duration timeWindow;
    private final int maxChainDepth;

    public RoundTripRule() {
        this(DEFAULT_TIME_WINDOW, DEFAULT_MAX_CHAIN_DEPTH);
    }

    /** Visible for testing — allows injecting custom parameters. */
    RoundTripRule(Duration timeWindow, int maxChainDepth) {
        this.timeWindow = timeWindow;
        this.maxChainDepth = maxChainDepth;
    }

    @Override
    public RuleName name() {
        return RuleName.ROUND_TRIP;
    }

    @Override
    public Optional<Alert> evaluate(Transaction transaction, List<Transaction> history) {
        Instant cutoff = transaction.timestamp().minus(timeWindow);

        // Filter history to recent transactions within the time window
        List<Transaction> recentHistory = history.stream()
                .filter(t -> t.timestamp().isAfter(cutoff))
                .toList();

        // BFS: start from the destination account, look for a path back to the source
        String sourceAccount = transaction.fromAccount();
        String startAccount = transaction.toAccount();

        // Trivial self-transfer — not a round-trip pattern
        if (sourceAccount.equals(startAccount)) {
            return Optional.empty();
        }

        List<String> chain = findReturnPath(startAccount, sourceAccount, recentHistory);

        if (chain != null) {
            return Optional.of(Alert.raise(
                    transaction,
                    name(),
                    RiskLevel.HIGH,
                    String.format(
                            "Circular money flow detected: %s → %s → %s (chain depth: %d, window: %dh)",
                            sourceAccount,
                            String.join(" → ", chain),
                            sourceAccount,
                            chain.size() + 1,
                            timeWindow.toHours())));
        }

        return Optional.empty();
    }

    /**
     * BFS to find a path from startAccount back to targetAccount through
     * transaction history.
     *
     * @return list of intermediate accounts forming the path, or null if no path
     */
    private List<String> findReturnPath(String startAccount, String targetAccount,
            List<Transaction> history) {

        // Queue entries: (current account, path so far)
        Deque<PathEntry> queue = new ArrayDeque<>();
        queue.add(new PathEntry(startAccount, List.of(startAccount)));

        Set<String> visited = new HashSet<>();
        visited.add(startAccount);

        while (!queue.isEmpty()) {
            PathEntry current = queue.poll();

            if (current.path.size() > maxChainDepth) {
                continue;
            }

            for (Transaction t : history) {
                if (!t.fromAccount().equals(current.account)) {
                    continue;
                }

                String nextAccount = t.toAccount();

                if (nextAccount.equals(targetAccount)) {
                    // Found the return path
                    return current.path;
                }

                if (!visited.contains(nextAccount)) {
                    visited.add(nextAccount);
                    List<String> newPath = new ArrayList<>(current.path);
                    newPath.add(nextAccount);
                    queue.add(new PathEntry(nextAccount, newPath));
                }
            }
        }

        return null;
    }

    private record PathEntry(String account, List<String> path) {
    }
}
