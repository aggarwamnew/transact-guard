package com.transactguard.store;

import com.transactguard.model.Transaction;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory transaction store with per-account indexing.
 * Will be replaced by DynamoDB in Day 8.
 */
@Component
public class TransactionStore {

    private final Map<String, Transaction> byId = new ConcurrentHashMap<>();
    private final Map<String, List<Transaction>> byAccount = new ConcurrentHashMap<>();

    public Transaction save(Transaction transaction) {
        byId.put(transaction.id(), transaction);

        byAccount.computeIfAbsent(transaction.fromAccount(), k -> new ArrayList<>())
                .add(transaction);

        if (!transaction.fromAccount().equals(transaction.toAccount())) {
            byAccount.computeIfAbsent(transaction.toAccount(), k -> new ArrayList<>())
                    .add(transaction);
        }

        return transaction;
    }

    public Optional<Transaction> findById(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    /**
     * Returns all transactions involving this account (as sender or receiver).
     * Used by RuleEngine to build account history for contextual rules.
     */
    public List<Transaction> findByAccount(String accountId) {
        return byAccount.getOrDefault(accountId, Collections.emptyList());
    }

    public List<Transaction> findAll() {
        return List.copyOf(byId.values());
    }

    public long count() {
        return byId.size();
    }
}
