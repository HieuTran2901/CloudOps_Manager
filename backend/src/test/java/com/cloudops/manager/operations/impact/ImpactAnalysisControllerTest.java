package com.cloudops.manager.operations.impact;

import com.cloudops.manager.aws.topology.model.TopologyNodeType;
import com.cloudops.manager.operations.impact.controller.ImpactAnalysisController;
import com.cloudops.manager.operations.impact.model.ImpactAnalysisResult;
import com.cloudops.manager.operations.impact.model.ImpactAnalysisStatus;
import com.cloudops.manager.operations.impact.model.ImpactResourceSummary;
import com.cloudops.manager.operations.impact.service.BlastRadiusAnalysisEngine;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ImpactAnalysisController.class)
class ImpactAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean(name = "changeImpactBlastRadiusAnalysisEngine")
    private BlastRadiusAnalysisEngine blastRadiusAnalysisEngine;

    @Test
    @DisplayName("GET /api/v1/impact/blast-radius returns HTTP 200 with standard ApiResponse envelope")
    void testGetBlastRadiusEndpoint() throws Exception {
        ImpactResourceSummary target = new ImpactResourceSummary(
                "351405419700:ap-southeast-2:VPC:vpc-123",
                TopologyNodeType.VPC, "vpc-123", "351405419700", "ap-southeast-2", 0, false, Map.of()
        );

        ImpactAnalysisResult result = new ImpactAnalysisResult(
                target, "351405419700", "ap-southeast-2", 3,
                1, 1, 0, Map.of("SUBNET", 1), List.of(), List.of(), List.of(),
                ImpactAnalysisStatus.SUCCESS, List.of("PUBLIC_PATH_ANALYSIS = NOT_SUPPORTED"), Instant.now()
        );

        when(blastRadiusAnalysisEngine.analyzeBlastRadius(anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(result);

        mockMvc.perform(get("/api/v1/impact/blast-radius")
                        .param("resourceType", "VPC")
                        .param("resourceId", "vpc-123")
                        .param("region", "ap-southeast-2")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.targetResource.resourceId").value("vpc-123"))
                .andExpect(jsonPath("$.data.totalAffectedResources").value(1))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("GET /api/v1/impact/blast-radius returns HTTP 200 with AMBIGUOUS_RESOURCE status when duplicate resources exist")
    void testGetBlastRadiusAmbiguousResource() throws Exception {
        ImpactAnalysisResult result = ImpactAnalysisResult.empty(
                "351405419700", "ap-southeast-2", ImpactAnalysisStatus.AMBIGUOUS_RESOURCE,
                "Ambiguous target resource: found 2 resources of type EC2_INSTANCE matching id 'i-dup'."
        );

        when(blastRadiusAnalysisEngine.analyzeBlastRadius(anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(result);

        mockMvc.perform(get("/api/v1/impact/blast-radius")
                        .param("resourceType", "EC2_INSTANCE")
                        .param("resourceId", "i-dup")
                        .param("region", "ap-southeast-2")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("AMBIGUOUS_RESOURCE"))
                .andExpect(jsonPath("$.data.totalAffectedResources").value(0));
    }
}
