package com.cloudops.manager.operations.risk;

import com.cloudops.manager.operations.risk.controller.RiskAssessmentController;
import com.cloudops.manager.operations.risk.model.*;
import com.cloudops.manager.operations.risk.service.RiskAssessmentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RiskAssessmentController.class)
class RiskAssessmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RiskAssessmentService riskAssessmentService;

    @Test
    @DisplayName("GET /api/v1/risks returns standard ApiResponse with risk report")
    void testGetOperationalRisksApiContract() throws Exception {
        RecommendedAction action = new RecommendedAction(
                "request-ec2-vcpu-quota-increase",
                "Request EC2 vCPU quota increase",
                "Submit quota increase request in AWS Service Quotas console.",
                ActionSafety.REQUIRES_APPROVAL,
                List.of("Step 1", "Step 2"),
                "Verify utilization drops"
        );

        OperationalRisk risk = new OperationalRisk(
                "risk-quota-capacity-L-1216C47A",
                RiskCategory.CAPACITY,
                RiskSeverity.CRITICAL,
                "EC2 capacity exhaustion risk",
                "vCPU capacity at 100%",
                "Autoscaling blocked",
                List.of("L-1216C47A"),
                Map.of("currentUsage", 32.0, "appliedLimit", 32.0, "utilizationPercentage", 100.0),
                Instant.now(),
                action,
                RiskSource.QUOTA
        );

        RiskAssessmentReport report = new RiskAssessmentReport(
                "351405419700",
                "ap-southeast-2",
                1,
                1,
                0,
                0,
                0,
                List.of(risk),
                Instant.now()
        );

        when(riskAssessmentService.getRiskAssessment(any(), any(), any())).thenReturn(report);

        mockMvc.perform(get("/api/v1/risks?region=ap-southeast-2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accountId").value("351405419700"))
                .andExpect(jsonPath("$.data.region").value("ap-southeast-2"))
                .andExpect(jsonPath("$.data.totalRisksTracked").value(1))
                .andExpect(jsonPath("$.data.criticalCount").value(1))
                .andExpect(jsonPath("$.data.risks[0].riskId").value("risk-quota-capacity-L-1216C47A"))
                .andExpect(jsonPath("$.data.risks[0].severity").value("CRITICAL"))
                .andExpect(jsonPath("$.data.risks[0].category").value("CAPACITY"))
                .andExpect(jsonPath("$.data.risks[0].action.safetyLevel").value("REQUIRES_APPROVAL"));
    }
}
