package com.transactguard.controller;

import com.transactguard.model.Alert;
import com.transactguard.model.Transaction;
import com.transactguard.service.TransactionService;
import com.transactguard.store.TransactionStore;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for transaction ingestion and lookup.
 */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionStore transactionStore;

    public TransactionController(TransactionService transactionService,
            TransactionStore transactionStore) {
        this.transactionService = transactionService;
        this.transactionStore = transactionStore;
    }

    /**
     * Ingest a new transaction through the AML pipeline.
     * Returns the transaction and any alerts raised.
     */
    @PostMapping
    public ResponseEntity<IngestResponse> ingest(@Valid @RequestBody TransactionRequest request) {
        Transaction transaction = Transaction.of(
                request.fromAccount(),
                request.toAccount(),
                request.amount(),
                request.currency());

        List<Alert> alerts = transactionService.ingest(transaction);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(IngestResponse.of(transaction, alerts));
    }

    /**
     * Look up a specific transaction by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Transaction> findById(@PathVariable String id) {
        return transactionStore.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
