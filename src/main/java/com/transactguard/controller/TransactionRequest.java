package com.transactguard.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Request DTO for transaction ingestion.
 * Bean Validation ensures data quality before processing.
 */
public record TransactionRequest(
        @NotBlank(message = "fromAccount is required") String fromAccount,

        @NotBlank(message = "toAccount is required") String toAccount,

        @NotNull(message = "amount is required") @Positive(message = "amount must be positive") BigDecimal amount,

        @NotBlank(message = "currency is required") String currency) {
}
