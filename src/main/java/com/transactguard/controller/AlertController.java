package com.transactguard.controller;

import com.transactguard.model.Alert;
import com.transactguard.model.RiskLevel;
import com.transactguard.store.AlertStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for alert querying and filtering.
 */
@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertStore alertStore;

    public AlertController(AlertStore alertStore) {
        this.alertStore = alertStore;
    }

    /**
     * Query all alerts, optionally filtered by risk level or status.
     */
    @GetMapping
    public ResponseEntity<List<Alert>> findAll(
            @RequestParam(required = false) RiskLevel riskLevel,
            @RequestParam(required = false) Alert.AlertStatus status) {

        List<Alert> alerts;

        if (riskLevel != null) {
            alerts = alertStore.findByRiskLevel(riskLevel);
        } else if (status != null) {
            alerts = alertStore.findByStatus(status);
        } else {
            alerts = alertStore.findAll();
        }

        return ResponseEntity.ok(alerts);
    }

    /**
     * Look up a specific alert by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Alert> findById(@PathVariable String id) {
        return alertStore.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
