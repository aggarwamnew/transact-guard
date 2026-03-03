package com.transactguard.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents a financial transaction submitted for AML screening.
 */
public record Transaction(
        String id,
        String fromAccount,
        String toAccount,
        BigDecimal amount,
        String currency,
        Instant timestamp) {
    /**
     * Create a transaction with an auto-generated ID and current timestamp.
     */
    public static Transaction of(String from, String to, BigDecimal amount, String currency) {
        return new Transaction(
                UUID.randomUUID().toString(),
                from, to, amount, currency,
                Instant.now());
    }
}
