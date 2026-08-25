package com.cloudops.manager.aws.observability.controller;

import com.cloudops.manager.aws.observability.model.TelemetryAggregationResult;
import com.cloudops.manager.aws.observability.service.AwsObservabilityService;
import com.cloudops.manager.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AwsObservabilityController.class)
@Import(GlobalExceptionHandler.class)
class AwsObservabilityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AwsObservabilityService observabilityService;

    @Test
    @DisplayName("GET /api/v1/aws/observability/metrics should return TelemetryAggregationResult")
    void shouldReturnTelemetryMetrics() throws Exception {
        TelemetryAggregationResult result = new TelemetryAggregationResult(
                "123456789012", "us-east-1", Instant.now().minusSeconds(3600), Instant.now(),
                1, 5, List.of(), Instant.now()
        );

        when(observabilityService.getAggregatedMetrics(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(result);

        mockMvc.perform(get("/api/v1/aws/observability/metrics?resourceType=EC2&resourceIds=i-123&metricNames=CPUUtilization")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accountId").value("123456789012"));
    }
}