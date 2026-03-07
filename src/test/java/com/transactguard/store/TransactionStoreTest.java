package com.transactguard.store;

import com.transactguard.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TransactionStoreTest {

    private TransactionStore store;

    @BeforeEach
    void setUp() {
        store = new TransactionStore();
    }

    @Test
    void save_and_findById() {
        Transaction tx = Transaction.of("ACC-001", "ACC-002", new BigDecimal("5000"), "EUR");
        store.save(tx);

        Optional<Transaction> found = store.findById(tx.id());
        assertTrue(found.isPresent());
        assertEquals(tx.id(), found.get().id());
        assertEquals(new BigDecimal("5000"), found.get().amount());
    }

    @Test
    void findById_notFound_returnsEmpty() {
        assertTrue(store.findById("nonexistent").isEmpty());
    }

    @Test
    void findByAccount_returnsSenderTransactions() {
        Transaction tx1 = Transaction.of("ACC-001", "ACC-002", new BigDecimal("1000"), "EUR");
        Transaction tx2 = Transaction.of("ACC-001", "ACC-003", new BigDecimal("2000"), "EUR");
        Transaction tx3 = Transaction.of("ACC-999", "ACC-002", new BigDecimal("3000"), "EUR");
        store.save(tx1);
        store.save(tx2);
        store.save(tx3);

        List<Transaction> acc001History = store.findByAccount("ACC-001");
        assertEquals(2, acc001History.size());
    }

    @Test
    void findByAccount_returnsReceiverTransactions() {
        Transaction tx = Transaction.of("ACC-001", "ACC-002", new BigDecimal("1000"), "EUR");
        store.save(tx);

        // ACC-002 is the receiver — should also appear in their history
        List<Transaction> acc002History = store.findByAccount("ACC-002");
        assertEquals(1, acc002History.size());
    }

    @Test
    void findByAccount_unknownAccount_returnsEmptyList() {
        assertTrue(store.findByAccount("UNKNOWN").isEmpty());
    }

    @Test
    void count_tracksStoredTransactions() {
        assertEquals(0, store.count());
        store.save(Transaction.of("A", "B", BigDecimal.TEN, "EUR"));
        store.save(Transaction.of("C", "D", BigDecimal.ONE, "USD"));
        assertEquals(2, store.count());
    }
}
