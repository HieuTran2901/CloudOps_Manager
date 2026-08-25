package com.cloudops.manager.operations.controller;

import com.cloudops.manager.operations.model.AwsConnectivityStatus;
import com.cloudops.manager.operations.model.AwsOperationalStatus;
import com.cloudops.manager.operations.model.OperationalEvent;
import com.cloudops.manager.operations.model.OperationalSeverity;
import com.cloudops.manager.operations.service.OperationalEventBuffer;
import com.cloudops.manager.operations.service.OperationsMonitoringService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OperationsController.class)
class OperationsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OperationsMonitoringService operationsService;

    @Test
    @DisplayName("GET /api/v1/operations/status returns operational status")
    void testGetOperationalStatus() throws Exception {
        AwsOperationalStatus mockStatus = new AwsOperationalStatus(
                AwsConnectivityStatus.CONNECTED,
                "123456789012",
                "us-east-1",
                Instant.now(),
                Instant.now(),
                0L,
                "AWS identity verified successfully.",
                Map.of("version", "1.0.0")
        );
        when(operationsService.getAwsOperationalStatus(any())).thenReturn(mockStatus);

        mockMvc.perform(get("/api/v1/operations/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CONNECTED"))
                .andExpect(jsonPath("$.data.accountId").value("123456789012"));
    }

    @Test
    @DisplayName("GET /api/v1/operations/events returns event list")
    void testGetOperationalEvents() throws Exception {
        OperationalEventBuffer mockBuffer = new OperationalEventBuffer();
        mockBuffer.recordEvent("TEST_EVENT", OperationalSeverity.INFO, "Test event message", "TEST", Map.of());
        when(operationsService.getEventBuffer()).thenReturn(mockBuffer);

        mockMvc.perform(get("/api/v1/operations/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/operations/freshness returns evidence freshness")
    void testGetEvidenceFreshness() throws Exception {
        AwsOperationalStatus mockStatus = new AwsOperationalStatus(
                AwsConnectivityStatus.CONNECTED,
                "123456789012",
                "us-east-1",
                Instant.now(),
                Instant.now(),
                12L,
                "OK",
                Map.of()
        );
        when(operationsService.getAwsOperationalStatus(any())).thenReturn(mockStatus);

        mockMvc.perform(get("/api/v1/operations/freshness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CONNECTED"))
                .andExpect(jsonPath("$.data.evidenceAgeSeconds").value(12));
    }
}