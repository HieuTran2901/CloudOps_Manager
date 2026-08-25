package com.cloudops.manager.verification.contract;

import com.cloudops.manager.aws.discovery.controller.AwsResourceDiscoveryController;
import com.cloudops.manager.aws.discovery.model.InventorySummary;
import com.cloudops.manager.aws.discovery.service.AwsResourceDiscoveryService;
import com.cloudops.manager.aws.observability.service.AwsObservabilityService;
import com.cloudops.manager.aws.security.controller.SecurityAnalysisController;
import com.cloudops.manager.aws.security.model.BlastRadiusResult;
import com.cloudops.manager.aws.security.model.ReachabilityStatus;
import com.cloudops.manager.aws.security.model.SecurityPath;
import com.cloudops.manager.aws.security.model.SecurityReachabilityResult;
import com.cloudops.manager.aws.security.service.SecurityAnalysisService;
import com.cloudops.manager.aws.topology.controller.TopologyController;
import com.cloudops.manager.aws.topology.model.TopologyGraph;
import com.cloudops.manager.aws.topology.service.TopologyQueryService;
import com.cloudops.manager.common.exception.GlobalExceptionHandler;
import com.cloudops.manager.common.health.HealthController;
import com.cloudops.manager.operations.model.DetailedHealthResponse;
import com.cloudops.manager.operations.model.HealthStatus;
import com.cloudops.manager.operations.service.OperationsMonitoringService;
import com.cloudops.manager.verification.fixture.SyntheticEvidenceFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({
        HealthController.class,
        AwsResourceDiscoveryController.class,
        TopologyController.class,
        SecurityAnalysisController.class
})
@Import(GlobalExceptionHandler.class)
@DisplayName("API Contract & Sensitive Field Verification")
class ApiContractVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OperationsMonitoringService operationsService;

    @MockBean
    private AwsResourceDiscoveryService discoveryService;

    @MockBean
    private AwsObservabilityService observabilityService;

    @MockBean
    private TopologyQueryService topologyQueryService;

    @MockBean
    private SecurityAnalysisService securityAnalysisService;

    @Test
    @DisplayName("GET /api/v1/health conforms to health contract")
    void testHealthContract() throws Exception {
        DetailedHealthResponse mockHealth = new DetailedHealthResponse(
                "UP",
                "cloudops-manager",
                "1.0.0",
                "release-2026.08-p38",
                Map.of("application", HealthStatus.UP),
                Instant.now()
        );
        when(operationsService.getDetailedHealth()).thenReturn(mockHealth);

        MvcResult result = mockMvc.perform(get("/api/v1/health").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.service").value("cloudops-manager"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertFalse(body.contains("Authorization"));
        assertFalse(body.contains("AccessKeyId"));
        assertFalse(body.contains("SecretAccessKey"));
        assertFalse(body.contains("SessionToken"));
        assertFalse(body.contains("stackTrace"));
    }

    @Test
    @DisplayName("GET /api/v1/aws/resources conforms to ApiResponse<InventorySummary> contract")
    void testDiscoveryContract() throws Exception {
        InventorySummary summary = SyntheticEvidenceFixtures.createSyntheticInventory(
                SyntheticEvidenceFixtures.SYNTHETIC_ACCOUNT_A,
                SyntheticEvidenceFixtures.REGION_US_EAST_1,
                2
        );
        when(discoveryService.discoverAll(any())).thenReturn(summary);

        mockMvc.perform(get("/api/v1/aws/resources")
                        .param("region", SyntheticEvidenceFixtures.REGION_US_EAST_1)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.resources").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/aws/topology conforms to ApiResponse<TopologyGraph> contract")
    void testTopologyContract() throws Exception {
        TopologyGraph graph = SyntheticEvidenceFixtures.createSyntheticGraph(
                SyntheticEvidenceFixtures.SYNTHETIC_ACCOUNT_A,
                SyntheticEvidenceFixtures.REGION_US_EAST_1
        );
        when(topologyQueryService.getTopology(any())).thenReturn(graph);

        mockMvc.perform(get("/api/v1/aws/topology")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nodeCount").value(4))
                .andExpect(jsonPath("$.data.edgeCount").value(3))
                .andExpect(jsonPath("$.data.nodes").isArray())
                .andExpect(jsonPath("$.data.edges").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/aws/security/blast-radius/{nodeId} conforms to BlastRadiusResult contract")
    void testBlastRadiusContract() throws Exception {
        BlastRadiusResult result = new BlastRadiusResult(
                "synthetic-node-01",
                3,
                List.of(),
                List.of(),
                2,
                2,
                "123",
                "us-east-1"
        );
        when(securityAnalysisService.getBlastRadius(anyString(), anyInt(), any()))
                .thenReturn(result);

        mockMvc.perform(get("/api/v1/aws/security/blast-radius/synthetic-node-01")
                        .param("maxDepth", "3")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sourceNodeId").value("synthetic-node-01"))
                .andExpect(jsonPath("$.data.traversedNodeCount").value(2))
                .andExpect(jsonPath("$.data.traversedEdgeCount").value(2));
    }

    @Test
    @DisplayName("GET /api/v1/aws/security/reachability conforms to SecurityReachabilityResult contract")
    void testReachabilityContract() throws Exception {
        SecurityPath path = new SecurityPath(
                "synthetic-node-01",
                "synthetic-node-03",
                List.of("synthetic-node-01", "synthetic-node-02", "synthetic-node-03"),
                List.of(),
                2,
                "123",
                "us-east-1"
        );
        SecurityReachabilityResult result = new SecurityReachabilityResult(
                "synthetic-node-01",
                "synthetic-node-03",
                ReachabilityStatus.REACHABLE,
                path,
                5,
                "123",
                "us-east-1"
        );
        when(securityAnalysisService.getReachability(anyString(), anyString(), anyInt(), any()))
                .thenReturn(result);

        mockMvc.perform(get("/api/v1/aws/security/reachability")
                        .param("from", "synthetic-node-01")
                        .param("to", "synthetic-node-03")
                        .param("maxDepth", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("REACHABLE"))
                .andExpect(jsonPath("$.data.path.nodeIds").isArray());
    }
}