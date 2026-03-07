package com.transactguard.store;

import com.transactguard.model.Alert;
import com.transactguard.model.RiskLevel;
import com.transactguard.model.Transaction;
import com.transactguard.rule.RuleName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AlertStoreTest {

    private AlertStore store;

    @BeforeEach
    void setUp() {
        store = new AlertStore();
    }

    private Alert createAlert(RiskLevel level, Alert.AlertStatus status) {
        Transaction tx = Transaction.of("ACC-001", "ACC-002", new BigDecimal("50000"), "EUR");
        return new Alert(
                java.util.UUID.randomUUID().toString(),
                tx.id(),
                RuleName.HIGH_VALUE,
                level,
                "Test alert",
                status,
                java.time.Instant.now());
    }

    @Test
    void saveAll_and_findAll() {
        List<Alert> alerts = List.of(
                createAlert(RiskLevel.HIGH, Alert.AlertStatus.PENDING_REVIEW),
                createAlert(RiskLevel.MEDIUM, Alert.AlertStatus.PENDING_REVIEW));
        store.saveAll(alerts);

        assertEquals(2, store.findAll().size());
        assertEquals(2, store.count());
    }

    @Test
    void findById_existing() {
        Alert alert = createAlert(RiskLevel.HIGH, Alert.AlertStatus.PENDING_REVIEW);
        store.saveAll(List.of(alert));

        assertTrue(store.findById(alert.id()).isPresent());
        assertEquals(alert.id(), store.findById(alert.id()).get().id());
    }

    @Test
    void findById_notFound() {
        assertTrue(store.findById("nonexistent").isEmpty());
    }

    @Test
    void findByRiskLevel_filtersCorrectly() {
        store.saveAll(List.of(
                createAlert(RiskLevel.HIGH, Alert.AlertStatus.PENDING_REVIEW),
                createAlert(RiskLevel.HIGH, Alert.AlertStatus.PENDING_REVIEW),
                createAlert(RiskLevel.MEDIUM, Alert.AlertStatus.PENDING_REVIEW),
                createAlert(RiskLevel.LOW, Alert.AlertStatus.PENDING_REVIEW)));

        assertEquals(2, store.findByRiskLevel(RiskLevel.HIGH).size());
        assertEquals(1, store.findByRiskLevel(RiskLevel.MEDIUM).size());
        assertEquals(1, store.findByRiskLevel(RiskLevel.LOW).size());
        assertEquals(0, store.findByRiskLevel(RiskLevel.CRITICAL).size());
    }

    @Test
    void findByStatus_filtersCorrectly() {
        store.saveAll(List.of(
                createAlert(RiskLevel.HIGH, Alert.AlertStatus.PENDING_REVIEW),
                createAlert(RiskLevel.HIGH, Alert.AlertStatus.ESCALATED),
                createAlert(RiskLevel.MEDIUM, Alert.AlertStatus.DISMISSED)));

        assertEquals(1, store.findByStatus(Alert.AlertStatus.PENDING_REVIEW).size());
        assertEquals(1, store.findByStatus(Alert.AlertStatus.ESCALATED).size());
        assertEquals(1, store.findByStatus(Alert.AlertStatus.DISMISSED).size());
        assertEquals(0, store.findByStatus(Alert.AlertStatus.REVIEWED).size());
    }
}
