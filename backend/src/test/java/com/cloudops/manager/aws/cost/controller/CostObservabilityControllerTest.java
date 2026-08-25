package com.cloudops.manager.aws.cost.controller;

import com.cloudops.manager.aws.cost.model.CostAggregationResult;
import com.cloudops.manager.aws.cost.model.CostTimePeriod;
import com.cloudops.manager.aws.cost.service.CostObservabilityService;
import com.cloudops.manager.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CostObservabilityController.class)
@Import(GlobalExceptionHandler.class)
class CostObservabilityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CostObservabilityService costObservabilityService;

    @Test
    @DisplayName("GET /api/v1/aws/costs should return CostAggregationResult")
    void shouldReturnCostAggregationResult() throws Exception {
        CostAggregationResult result = new CostAggregationResult(
                "123456789012", "STANDALONE_ACCOUNT", "UnblendedCost", "MONTHLY",
                new CostTimePeriod("2026-01-01", "2026-02-01"),
                new BigDecimal("123.45"), "USD", List.of(), Instant.now()
        );

        when(costObservabilityService.getCostAndUsage(any(), any(), any(), any(), any(), any()))
                .thenReturn(result);

        mockMvc.perform(get("/api/v1/aws/costs").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accountId").value("123456789012"))
                .andExpect(jsonPath("$.data.totalAmount").value(123.45));
    }
}