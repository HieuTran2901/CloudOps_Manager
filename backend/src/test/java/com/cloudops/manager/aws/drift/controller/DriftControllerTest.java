package com.cloudops.manager.aws.drift.controller;

import com.cloudops.manager.aws.drift.model.DriftReport;
import com.cloudops.manager.aws.drift.service.DriftComparisonService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DriftController.class)
class DriftControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DriftComparisonService driftService;

    @Test
    @DisplayName("GET /api/v1/aws/drift/supported-resources should return supported types")
    void shouldReturnSupportedResourceTypes() throws Exception {
        when(driftService.getSupportedResourceTypes())
                .thenReturn(List.of("aws_db_instance", "aws_instance", "aws_s3_bucket", "aws_security_group", "aws_vpc"));

        mockMvc.perform(get("/api/v1/aws/drift/supported-resources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[1]").value("aws_instance"));
    }

    @Test
    @DisplayName("POST /api/v1/aws/drift/evaluate should return drift report")
    void shouldEvaluateDrift() throws Exception {
        DriftReport report = new DriftReport("123456789012", "us-east-1", Instant.now(), 1, 1, 0, 0, 0, 0, List.of());
        when(driftService.evaluateDrift(any(), any())).thenReturn(report);

        mockMvc.perform(post("/api/v1/aws/drift/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\": 4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accountId").value("123456789012"));
    }
}