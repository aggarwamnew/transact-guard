package com.transactguard.store;

import com.transactguard.model.Alert;
import com.transactguard.model.RiskLevel;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory alert store with filtering.
 * Will be replaced by DynamoDB in Day 8.
 */
@Component
public class AlertStore {

    private final Map<String, Alert> byId = new ConcurrentHashMap<>();

    public void saveAll(List<Alert> alerts) {
        alerts.forEach(alert -> byId.put(alert.id(), alert));
    }

    public Optional<Alert> findById(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public List<Alert> findAll() {
        return List.copyOf(byId.values());
    }

    public List<Alert> findByRiskLevel(RiskLevel riskLevel) {
        return byId.values().stream()
                .filter(a -> a.riskLevel() == riskLevel)
                .collect(Collectors.toList());
    }

    public List<Alert> findByStatus(Alert.AlertStatus status) {
        return byId.values().stream()
                .filter(a -> a.status() == status)
                .collect(Collectors.toList());
    }

    public long count() {
        return byId.size();
    }
}
