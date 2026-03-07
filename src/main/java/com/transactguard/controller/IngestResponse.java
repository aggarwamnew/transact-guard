package com.transactguard.controller;

import com.transactguard.model.Alert;
import com.transactguard.model.Transaction;

import java.util.List;

/**
 * Response DTO for POST /api/transactions.
 * Returns the stored transaction plus any alerts raised.
 */
public record IngestResponse(
        Transaction transaction,
        List<Alert> alerts,
        int alertCount) {
    public static IngestResponse of(Transaction transaction, List<Alert> alerts) {
        return new IngestResponse(transaction, alerts, alerts.size());
    }
}
