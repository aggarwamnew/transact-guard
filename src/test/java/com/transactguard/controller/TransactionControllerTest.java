package com.transactguard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void postTransaction_validRequest_returns201WithAlerts() throws Exception {
        String body = """
                {
                    "fromAccount": "ACC-001",
                    "toAccount": "ACC-002",
                    "amount": 50000,
                    "currency": "EUR"
                }
                """;

        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transaction.fromAccount").value("ACC-001"))
                .andExpect(jsonPath("$.transaction.toAccount").value("ACC-002"))
                .andExpect(jsonPath("$.transaction.amount").value(50000))
                .andExpect(jsonPath("$.alerts").isArray())
                .andExpect(jsonPath("$.alertCount").isNumber());
    }

    @Test
    void postTransaction_highValue_triggersAlert() throws Exception {
        String body = """
                {
                    "fromAccount": "ACC-HV-001",
                    "toAccount": "ACC-HV-002",
                    "amount": 15000,
                    "currency": "EUR"
                }
                """;

        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.alertCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.alerts[0].ruleName").value("HIGH_VALUE"));
    }

    @Test
    void postTransaction_missingFromAccount_returns400() throws Exception {
        String body = """
                {
                    "toAccount": "ACC-002",
                    "amount": 5000,
                    "currency": "EUR"
                }
                """;

        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postTransaction_negativeAmount_returns400() throws Exception {
        String body = """
                {
                    "fromAccount": "ACC-001",
                    "toAccount": "ACC-002",
                    "amount": -100,
                    "currency": "EUR"
                }
                """;

        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postTransaction_missingAmount_returns400() throws Exception {
        String body = """
                {
                    "fromAccount": "ACC-001",
                    "toAccount": "ACC-002",
                    "currency": "EUR"
                }
                """;

        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTransaction_afterPost_returns200() throws Exception {
        String body = """
                {
                    "fromAccount": "ACC-GET-001",
                    "toAccount": "ACC-GET-002",
                    "amount": 500,
                    "currency": "EUR"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        String txId = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("transaction").path("id").asText();

        mockMvc.perform(get("/api/transactions/" + txId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromAccount").value("ACC-GET-001"));
    }

    @Test
    void getTransaction_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/transactions/nonexistent-id"))
                .andExpect(status().isNotFound());
    }
}
