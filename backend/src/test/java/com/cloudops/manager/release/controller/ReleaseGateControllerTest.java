package com.cloudops.manager.release.controller;

import com.cloudops.manager.release.model.ReleaseGateCheck;
import com.cloudops.manager.release.model.ReleaseGateResult;
import com.cloudops.manager.release.model.ReleaseGateSeverity;
import com.cloudops.manager.release.model.ReleaseGateStatus;
import com.cloudops.manager.release.service.ReleaseGateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReleaseGateController.class)
class ReleaseGateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReleaseGateService releaseGateService;

    @Test
    @DisplayName("GET /api/v1/release/gate returns structured ReleaseGateResult contract with runtimeReady")
    void testGetReleaseGateEndpoint() throws Exception {
        ReleaseGateResult mockResult = new ReleaseGateResult(
                ReleaseGateStatus.BLOCKED,
                true,
                true,
                true,
                true,
                true,
                true,
                false,
                false,
                false,
                "1.0.0",
                "release-2026.08-p38",
                "351405419700",
                "ap-southeast-2",
                List.of(new ReleaseGateCheck("Build", "Backend Tests", ReleaseGateStatus.PASS, ReleaseGateSeverity.INFO, "PASS", "174 tests")),
                "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789",
                Instant.now(),
                "Analytics PASS, Deployment BLOCKED."
        );
        when(releaseGateService.evaluateReleaseGate(any())).thenReturn(mockResult);

        mockMvc.perform(get("/api/v1/release/gate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.overallStatus").value("BLOCKED"))
                .andExpect(jsonPath("$.data.analyticsReady").value(true))
                .andExpect(jsonPath("$.data.resilienceReady").value(true))
                .andExpect(jsonPath("$.data.deploymentReady").value(false))
                .andExpect(jsonPath("$.data.runtimeReady").value(false))
                .andExpect(jsonPath("$.data.sha256Digest").value("abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789"));
    }
}