package com.transactguard.model;

import com.transactguard.rule.RuleName;

import java.time.Instant;
import java.util.UUID;

/**
 * An alert raised when a transaction triggers an AML rule.
 */
public record Alert(
        String id,
        String transactionId,
        RuleName ruleName,
        RiskLevel riskLevel,
        String description,
        AlertStatus status,
        Instant createdAt) {
    public enum AlertStatus {
        PENDING_REVIEW,
        REVIEWED,
        DISMISSED,
        ESCALATED
    }

    /**
     * Create a new alert in PENDING_REVIEW status.
     */
    public static Alert raise(Transaction transaction, RuleName ruleName,
            RiskLevel riskLevel, String description) {
        return new Alert(
                UUID.randomUUID().toString(),
                transaction.id(),
                ruleName,
                riskLevel,
                description,
                AlertStatus.PENDING_REVIEW,
                Instant.now());
    }
}
