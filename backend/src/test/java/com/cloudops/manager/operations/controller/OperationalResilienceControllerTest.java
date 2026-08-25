package com.cloudops.manager.operations.controller;

import com.cloudops.manager.operations.evidence.service.EvidenceLifecycleService;
import com.cloudops.manager.operations.incident.service.IncidentManagementService;
import com.cloudops.manager.operations.resilience.model.OperationalResilienceEvaluation;
import com.cloudops.manager.operations.resilience.service.OperationalResilienceService;
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

@WebMvcTest(OperationalResilienceController.class)
class OperationalResilienceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IncidentManagementService incidentService;

    @MockBean
    private EvidenceLifecycleService evidenceService;

    @MockBean
    private OperationalResilienceService resilienceService;

    @Test
    @DisplayName("GET /api/v1/operations/resilience returns structured OperationalResilienceEvaluation")
    void testGetResilienceEndpoint() throws Exception {
        OperationalResilienceEvaluation mockEval = new OperationalResilienceEvaluation(
                "RESILIENT_WITH_DEPLOYMENT_BOUNDARY",
                true,
                Map.of("discoveryHealth", "PASS", "deploymentState", "BLOCKED"),
                List.of(),
                List.of(),
                "351405419700",
                "ap-southeast-2",
                "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789",
                Instant.now(),
                "Resilience verified."
        );
        when(resilienceService.evaluateResilience(any())).thenReturn(mockEval);

        mockMvc.perform(get("/api/v1/operations/resilience"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.overallScore").value("RESILIENT_WITH_DEPLOYMENT_BOUNDARY"))
                .andExpect(jsonPath("$.data.isResilient").value(true))
                .andExpect(jsonPath("$.data.canonicalDigest").value("abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789"));
    }
}