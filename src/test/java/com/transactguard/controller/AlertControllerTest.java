package com.transactguard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private void ingestHighValueTransaction(String from) throws Exception {
        String body = String.format("""
                {
                    "fromAccount": "%s",
                    "toAccount": "ACC-SINK",
                    "amount": 25000,
                    "currency": "EUR"
                }
                """, from);

        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void getAlerts_afterIngestion_returnsAlerts() throws Exception {
        ingestHighValueTransaction("ACC-ALERT-001");

        mockMvc.perform(get("/api/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())));
    }

    @Test
    void getAlerts_filterByRiskLevel_returnsFiltered() throws Exception {
        ingestHighValueTransaction("ACC-ALERT-002");

        mockMvc.perform(get("/api/alerts")
                .param("riskLevel", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getAlerts_filterByStatus_returnsFiltered() throws Exception {
        ingestHighValueTransaction("ACC-ALERT-003");

        mockMvc.perform(get("/api/alerts")
                .param("status", "PENDING_REVIEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getAlert_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/alerts/nonexistent-id"))
                .andExpect(status().isNotFound());
    }
}
