package com.cloudops.manager.common.health;

import com.cloudops.manager.operations.model.DetailedHealthResponse;
import com.cloudops.manager.operations.model.HealthStatus;
import com.cloudops.manager.operations.service.OperationsMonitoringService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OperationsMonitoringService operationsService;

    @Test
    @DisplayName("GET /api/v1/health should return UP status and release metadata")
    void shouldReturnUpStatusAndMetadata() throws Exception {
        DetailedHealthResponse mockHealth = new DetailedHealthResponse(
                "UP",
                "cloudops-manager",
                "1.0.0",
                "release-2026.08-p38",
                Map.of("application", HealthStatus.UP, "aws", HealthStatus.UP),
                Instant.now()
        );
        when(operationsService.getDetailedHealth()).thenReturn(mockHealth);

        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.service").value("cloudops-manager"))
                .andExpect(jsonPath("$.data.version").value("1.0.0"))
                .andExpect(jsonPath("$.data.release").value("release-2026.08-p38"))
                .andExpect(jsonPath("$.data.components.application").value("UP"));
    }

    @Test
    @DisplayName("GET /api/v1/health/live should return lightweight UP status")
    void shouldReturnLivenessStatus() throws Exception {
        mockMvc.perform(get("/api/v1/health/live"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    @DisplayName("GET /api/v1/health/ready should return readiness probe response")
    void shouldReturnReadinessStatus() throws Exception {
        DetailedHealthResponse mockHealth = new DetailedHealthResponse(
                "UP",
                "cloudops-manager",
                "1.0.0",
                "release-2026.08-p38",
                Map.of("application", HealthStatus.UP),
                Instant.now()
        );
        when(operationsService.getDetailedHealth()).thenReturn(mockHealth);

        mockMvc.perform(get("/api/v1/health/ready"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.ready").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }
}
