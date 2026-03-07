package com.transactguard.service;

import com.transactguard.model.Alert;
import com.transactguard.model.Transaction;
import com.transactguard.rule.RuleEngine;
import com.transactguard.store.AlertStore;
import com.transactguard.store.TransactionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orchestrates the transaction processing pipeline:
 * ingest → store → get history → evaluate rules → store alerts.
 */
@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionStore transactionStore;
    private final AlertStore alertStore;
    private final RuleEngine ruleEngine;

    public TransactionService(TransactionStore transactionStore,
            AlertStore alertStore,
            RuleEngine ruleEngine) {
        this.transactionStore = transactionStore;
        this.alertStore = alertStore;
        this.ruleEngine = ruleEngine;
    }

    /**
     * Process an incoming transaction through the full AML pipeline.
     *
     * @return list of alerts generated (empty if clean)
     */
    public List<Alert> ingest(Transaction transaction) {
        // 1. Store the transaction
        transactionStore.save(transaction);

        // 2. Get account history for contextual rules (structuring, velocity,
        // round-trip)
        List<Transaction> history = transactionStore.findByAccount(transaction.fromAccount());

        // 3. Evaluate all rules
        List<Alert> alerts = ruleEngine.evaluate(transaction, history);

        // 4. Persist any alerts
        if (!alerts.isEmpty()) {
            alertStore.saveAll(alerts);
            log.info("AUDIT | Transaction processed | txId={} | alertsRaised={}",
                    transaction.id(), alerts.size());
        } else {
            log.info("AUDIT | Transaction processed | txId={} | clean=true",
                    transaction.id());
        }

        return alerts;
    }
}
