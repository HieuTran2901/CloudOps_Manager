package com.cloudops.manager.operations.impact;

import com.cloudops.manager.aws.sts.model.CallerIdentity;
import com.cloudops.manager.aws.sts.service.AwsIdentityService;
import com.cloudops.manager.aws.topology.model.*;
import com.cloudops.manager.aws.topology.service.TopologyQueryService;
import com.cloudops.manager.operations.impact.model.*;
import com.cloudops.manager.operations.impact.service.BlastRadiusAnalysisEngine;
import com.cloudops.manager.operations.impact.service.TopologyResourceIdentityResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlastRadiusAnalysisEngineTest {

    @Mock
    private TopologyQueryService topologyQueryService;

    @Mock
    private AwsIdentityService identityService;

    private TopologyResourceIdentityResolver identityResolver;
    private BlastRadiusAnalysisEngine engine;

    @BeforeEach
    void setUp() {
        identityResolver = new TopologyResourceIdentityResolver();
        engine = new BlastRadiusAnalysisEngine(
                topologyQueryService,
                identityResolver,
                identityService,
                "ap-southeast-2"
        );
    }

    @Test
    @DisplayName("Test 1: Linear graph A -> B -> C (EC2 -> Subnet -> VPC)")
    void testLinearGraphTraversal() {
        TopologyNode ec2 = TopologyNode.of(TopologyNodeType.EC2_INSTANCE, "i-123", "351405419700", "ap-southeast-2", Map.of());
        TopologyNode subnet = TopologyNode.of(TopologyNodeType.SUBNET, "subnet-123", "351405419700", "ap-southeast-2", Map.of());
        TopologyNode vpc = TopologyNode.of(TopologyNodeType.VPC, "vpc-123", "351405419700", "ap-southeast-2", Map.of());

        // EC2 is in Subnet; Subnet is in VPC
        TopologyEdge e1 = TopologyEdge.of(TopologyRelationshipType.EC2_IN_SUBNET, ec2.nodeId(), subnet.nodeId(), "351405419700", "ap-southeast-2", Map.of());
        TopologyEdge e2 = TopologyEdge.of(TopologyRelationshipType.SUBNET_IN_VPC, subnet.nodeId(), vpc.nodeId(), "351405419700", "ap-southeast-2", Map.of());

        TopologyGraph graph = new TopologyGraph("351405419700", "ap-southeast-2", Instant.now(), 3, 2, List.of(ec2, subnet, vpc), List.of(e1, e2));
        when(topologyQueryService.getTopology(anyString())).thenReturn(graph);

        // Analyze VPC blast radius (Downstream dependents)
        ImpactAnalysisResult vpcResult = engine.analyzeBlastRadius("VPC", "vpc-123", "ap-southeast-2", "351405419700", 3);

        assertEquals(ImpactAnalysisStatus.SUCCESS, vpcResult.status());
        assertEquals(2, vpcResult.totalAffectedResources());
        assertEquals(1, vpcResult.directAffectedCount()); // Subnet at depth 1
        assertEquals(1, vpcResult.indirectAffectedCount()); // EC2 at depth 2
        assertEquals(2, vpcResult.downstreamDependents().size());
        assertEquals(0, vpcResult.upstreamDependencies().size());

        // Analyze EC2 upstream dependencies
        ImpactAnalysisResult ec2Result = engine.analyzeBlastRadius("EC2_INSTANCE", "i-123", "ap-southeast-2", "351405419700", 3);
        assertEquals(ImpactAnalysisStatus.SUCCESS, ec2Result.status());
        assertEquals(2, ec2Result.totalAffectedResources());
        assertEquals(2, ec2Result.upstreamDependencies().size());
        assertEquals(0, ec2Result.downstreamDependents().size());
    }

    @Test
    @DisplayName("Test 2: Diamond graph and minimum-depth classification")
    void testDiamondGraphTraversal() {
        TopologyNode nA = TopologyNode.of(TopologyNodeType.EC2_INSTANCE, "i-A", "351405419700", "ap-southeast-2", Map.of());
        TopologyNode nB = TopologyNode.of(TopologyNodeType.SECURITY_GROUP, "sg-B", "351405419700", "ap-southeast-2", Map.of());
        TopologyNode nC = TopologyNode.of(TopologyNodeType.SUBNET, "subnet-C", "351405419700", "ap-southeast-2", Map.of());
        TopologyNode nD = TopologyNode.of(TopologyNodeType.VPC, "vpc-D", "351405419700", "ap-southeast-2", Map.of());

        // A -> B, A -> C, B -> D, C -> D
        TopologyEdge eAB = TopologyEdge.of(TopologyRelationshipType.EC2_ATTACHED_SECURITY_GROUP, nA.nodeId(), nB.nodeId(), "351405419700", "ap-southeast-2", Map.of());
        TopologyEdge eAC = TopologyEdge.of(TopologyRelationshipType.EC2_IN_SUBNET, nA.nodeId(), nC.nodeId(), "351405419700", "ap-southeast-2", Map.of());
        TopologyEdge eBD = TopologyEdge.of(TopologyRelationshipType.SUBNET_IN_VPC, nB.nodeId(), nD.nodeId(), "351405419700", "ap-southeast-2", Map.of());
        TopologyEdge eCD = TopologyEdge.of(TopologyRelationshipType.SUBNET_IN_VPC, nC.nodeId(), nD.nodeId(), "351405419700", "ap-southeast-2", Map.of());

        TopologyGraph graph = new TopologyGraph("351405419700", "ap-southeast-2", Instant.now(), 4, 4, List.of(nA, nB, nC, nD), List.of(eAB, eAC, eBD, eCD));
        when(topologyQueryService.getTopology(anyString())).thenReturn(graph);

        ImpactAnalysisResult result = engine.analyzeBlastRadius("VPC", "vpc-D", "ap-southeast-2", "351405419700", 3);

        assertEquals(ImpactAnalysisStatus.SUCCESS, result.status());
        assertEquals(3, result.totalAffectedResources()); // B, C, A
        assertEquals(2, result.directAffectedCount()); // B, C at depth 1
        assertEquals(1, result.indirectAffectedCount()); // A at depth 2
        assertEquals(3, result.downstreamDependents().size());
    }

    @Test
    @DisplayName("Test 3: Cycle safety A -> B -> A")
    void testCycleSafety() {
        TopologyNode nA = TopologyNode.of(TopologyNodeType.SECURITY_GROUP, "sg-A", "351405419700", "ap-southeast-2", Map.of());
        TopologyNode nB = TopologyNode.of(TopologyNodeType.SECURITY_GROUP, "sg-B", "351405419700", "ap-southeast-2", Map.of());

        TopologyEdge eAB = TopologyEdge.of(TopologyRelationshipType.EC2_ATTACHED_SECURITY_GROUP, nA.nodeId(), nB.nodeId(), "351405419700", "ap-southeast-2", Map.of());
        TopologyEdge eBA = TopologyEdge.of(TopologyRelationshipType.EC2_ATTACHED_SECURITY_GROUP, nB.nodeId(), nA.nodeId(), "351405419700", "ap-southeast-2", Map.of());

        TopologyGraph graph = new TopologyGraph("351405419700", "ap-southeast-2", Instant.now(), 2, 2, List.of(nA, nB), List.of(eAB, eBA));
        when(topologyQueryService.getTopology(anyString())).thenReturn(graph);

        // Must terminate safely without infinite loop
        ImpactAnalysisResult result = engine.analyzeBlastRadius("SECURITY_GROUP", "sg-A", "ap-southeast-2", "351405419700", 5);

        assertEquals(ImpactAnalysisStatus.SUCCESS, result.status());
        assertEquals(1, result.totalAffectedResources()); // Only sg-B affected
        assertEquals(1, result.directAffectedCount());
    }

    @Test
    @DisplayName("Test 4: Disconnected target has zero blast radius")
    void testDisconnectedTarget() {
        TopologyNode nA = TopologyNode.of(TopologyNodeType.EC2_INSTANCE, "i-standalone", "351405419700", "ap-southeast-2", Map.of());
        TopologyGraph graph = new TopologyGraph("351405419700", "ap-southeast-2", Instant.now(), 1, 0, List.of(nA), List.of());
        when(topologyQueryService.getTopology(anyString())).thenReturn(graph);

        ImpactAnalysisResult result = engine.analyzeBlastRadius("EC2_INSTANCE", "i-standalone", "ap-southeast-2", "351405419700", 3);

        assertEquals(ImpactAnalysisStatus.SUCCESS, result.status());
        assertEquals(0, result.totalAffectedResources());
        assertEquals(0, result.directAffectedCount());
        assertEquals(0, result.indirectAffectedCount());
        assertTrue(result.downstreamDependents().isEmpty());
    }

    @Test
    @DisplayName("Test 5: Depth bounding and boundary enforcement")
    void testDepthBounding() {
        TopologyNode ec2 = TopologyNode.of(TopologyNodeType.EC2_INSTANCE, "i-1", "351405419700", "ap-southeast-2", Map.of());
        TopologyNode subnet = TopologyNode.of(TopologyNodeType.SUBNET, "subnet-1", "351405419700", "ap-southeast-2", Map.of());
        TopologyNode vpc = TopologyNode.of(TopologyNodeType.VPC, "vpc-1", "351405419700", "ap-southeast-2", Map.of());

        TopologyEdge e1 = TopologyEdge.of(TopologyRelationshipType.EC2_IN_SUBNET, ec2.nodeId(), subnet.nodeId(), "351405419700", "ap-southeast-2", Map.of());
        TopologyEdge e2 = TopologyEdge.of(TopologyRelationshipType.SUBNET_IN_VPC, subnet.nodeId(), vpc.nodeId(), "351405419700", "ap-southeast-2", Map.of());

        TopologyGraph graph = new TopologyGraph("351405419700", "ap-southeast-2", Instant.now(), 3, 2, List.of(ec2, subnet, vpc), List.of(e1, e2));
        when(topologyQueryService.getTopology(anyString())).thenReturn(graph);

        // maxDepth = 1 on VPC only returns Subnet, not EC2
        ImpactAnalysisResult depth1Result = engine.analyzeBlastRadius("VPC", "vpc-1", "ap-southeast-2", "351405419700", 1);
        assertEquals(1, depth1Result.totalAffectedResources());
        assertEquals(1, depth1Result.downstreamDependents().size());
        assertEquals("subnet-1", depth1Result.downstreamDependents().get(0).resourceId());
    }

    @Test
    @DisplayName("Test 6: Unsupported resource types return UNSUPPORTED_RESOURCE_TYPE status")
    void testUnsupportedResourceTypes() {
        ImpactAnalysisResult albResult = engine.analyzeBlastRadius("ALB", "app/cloudops-alb", "ap-southeast-2", "351405419700", 3);
        assertEquals(ImpactAnalysisStatus.UNSUPPORTED_RESOURCE_TYPE, albResult.status());

        ImpactAnalysisResult s3Result = engine.analyzeBlastRadius("S3", "my-bucket", "ap-southeast-2", "351405419700", 3);
        assertEquals(ImpactAnalysisStatus.UNSUPPORTED_RESOURCE_TYPE, s3Result.status());
    }

    @Test
    @DisplayName("Test 7: Unknown resource returns NOT_FOUND status")
    void testUnknownResource() {
        TopologyGraph graph = new TopologyGraph("351405419700", "ap-southeast-2", Instant.now(), 0, 0, List.of(), List.of());
        when(topologyQueryService.getTopology(anyString())).thenReturn(graph);

        ImpactAnalysisResult result = engine.analyzeBlastRadius("EC2_INSTANCE", "i-nonexistent", "ap-southeast-2", "351405419700", 3);
        assertEquals(ImpactAnalysisStatus.NOT_FOUND, result.status());
    }

    @Test
    @DisplayName("Test 8: Deterministic ordering across repeated runs")
    void testDeterministicOrdering() {
        TopologyNode nA = TopologyNode.of(TopologyNodeType.EC2_INSTANCE, "i-Z", "351405419700", "ap-southeast-2", Map.of());
        TopologyNode nB = TopologyNode.of(TopologyNodeType.EC2_INSTANCE, "i-A", "351405419700", "ap-southeast-2", Map.of());
        TopologyNode nSubnet = TopologyNode.of(TopologyNodeType.SUBNET, "subnet-1", "351405419700", "ap-southeast-2", Map.of());

        TopologyEdge e1 = TopologyEdge.of(TopologyRelationshipType.EC2_IN_SUBNET, nA.nodeId(), nSubnet.nodeId(), "351405419700", "ap-southeast-2", Map.of());
        TopologyEdge e2 = TopologyEdge.of(TopologyRelationshipType.EC2_IN_SUBNET, nB.nodeId(), nSubnet.nodeId(), "351405419700", "ap-southeast-2", Map.of());

        TopologyGraph graph = new TopologyGraph("351405419700", "ap-southeast-2", Instant.now(), 3, 2, List.of(nA, nB, nSubnet), List.of(e1, e2));
        when(topologyQueryService.getTopology(anyString())).thenReturn(graph);

        ImpactAnalysisResult run1 = engine.analyzeBlastRadius("SUBNET", "subnet-1", "ap-southeast-2", "351405419700", 3);
        ImpactAnalysisResult run2 = engine.analyzeBlastRadius("SUBNET", "subnet-1", "ap-southeast-2", "351405419700", 3);

        assertEquals(run1.downstreamDependents().get(0).resourceId(), run2.downstreamDependents().get(0).resourceId());
        assertEquals("i-A", run1.downstreamDependents().get(0).resourceId()); // Lexicographical ordering
        assertEquals("i-Z", run1.downstreamDependents().get(1).resourceId());
    }

    @Test
    @DisplayName("Test 9: Ambiguous resource (>1 match with same resourceType and case-insensitive resourceId) returns AMBIGUOUS_RESOURCE")
    void testAmbiguousResource() {
        TopologyNode n1 = TopologyNode.of(TopologyNodeType.EC2_INSTANCE, "i-dup", "111111111111", "ap-southeast-2", Map.of());
        TopologyNode n2 = TopologyNode.of(TopologyNodeType.EC2_INSTANCE, "I-DUP", "222222222222", "ap-southeast-2", Map.of());

        TopologyGraph graph = new TopologyGraph("351405419700", "ap-southeast-2", Instant.now(), 2, 0, List.of(n1, n2), List.of());
        when(topologyQueryService.getTopology(anyString())).thenReturn(graph);

        ImpactAnalysisResult result = engine.analyzeBlastRadius("EC2_INSTANCE", "i-dup", "ap-southeast-2", "351405419700", 3);

        assertEquals(ImpactAnalysisStatus.AMBIGUOUS_RESOURCE, result.status());
        assertNotEquals(ImpactAnalysisStatus.NOT_FOUND, result.status());
        assertEquals(0, result.totalAffectedResources());
        assertNotNull(result.warnings());
        assertFalse(result.warnings().isEmpty());
    }

    @Test
    @DisplayName("Test 10: Multi-cycle graph with independent cycles A <-> B and C <-> D")
    void testMultiCycleIndependentCyclesInCombinedGraph() {
        // Cycle 1: sg-A <-> sg-B
        TopologyNode nA = TopologyNode.of(TopologyNodeType.SECURITY_GROUP, "sg-A", "351405419700", "ap-southeast-2", Map.of());
        TopologyNode nB = TopologyNode.of(TopologyNodeType.SECURITY_GROUP, "sg-B", "351405419700", "ap-southeast-2", Map.of());
        TopologyEdge eAB = TopologyEdge.of(TopologyRelationshipType.EC2_ATTACHED_SECURITY_GROUP, nA.nodeId(), nB.nodeId(), "351405419700", "ap-southeast-2", Map.of());
        TopologyEdge eBA = TopologyEdge.of(TopologyRelationshipType.EC2_ATTACHED_SECURITY_GROUP, nB.nodeId(), nA.nodeId(), "351405419700", "ap-southeast-2", Map.of());

        // Cycle 2: sg-C <-> sg-D (independent)
        TopologyNode nC = TopologyNode.of(TopologyNodeType.SECURITY_GROUP, "sg-C", "351405419700", "ap-southeast-2", Map.of());
        TopologyNode nD = TopologyNode.of(TopologyNodeType.SECURITY_GROUP, "sg-D", "351405419700", "ap-southeast-2", Map.of());
        TopologyEdge eCD = TopologyEdge.of(TopologyRelationshipType.EC2_ATTACHED_SECURITY_GROUP, nC.nodeId(), nD.nodeId(), "351405419700", "ap-southeast-2", Map.of());
        TopologyEdge eDC = TopologyEdge.of(TopologyRelationshipType.EC2_ATTACHED_SECURITY_GROUP, nD.nodeId(), nC.nodeId(), "351405419700", "ap-southeast-2", Map.of());

        TopologyGraph graph = new TopologyGraph(
                "351405419700", "ap-southeast-2", Instant.now(), 4, 4,
                List.of(nA, nB, nC, nD),
                List.of(eAB, eBA, eCD, eDC)
        );
        when(topologyQueryService.getTopology(anyString())).thenReturn(graph);

        // Analyze sg-A
        ImpactAnalysisResult resultA = engine.analyzeBlastRadius("SECURITY_GROUP", "sg-A", "ap-southeast-2", "351405419700", 5);

        assertEquals(ImpactAnalysisStatus.SUCCESS, resultA.status());
        assertEquals(1, resultA.totalAffectedResources()); // Only sg-B affected
        assertEquals(1, resultA.directAffectedCount());
        assertEquals(0, resultA.indirectAffectedCount());

        // Verify independent cycle C <-> D is completely isolated
        assertTrue(resultA.downstreamDependents().stream().noneMatch(d -> d.resourceId().equals("sg-C") || d.resourceId().equals("sg-D")));
        assertTrue(resultA.upstreamDependencies().stream().noneMatch(u -> u.resourceId().equals("sg-C") || u.resourceId().equals("sg-D")));

        // Verify no duplicate paths
        Set<ImpactPath> pathSet = new HashSet<>(resultA.impactPaths());
        assertEquals(pathSet.size(), resultA.impactPaths().size(), "Impact paths must be deduplicated");

        // Verify no duplicate nodes
        Set<String> downstreamIds = new HashSet<>(resultA.downstreamDependents().stream().map(ImpactResourceSummary::resourceId).toList());
        assertEquals(downstreamIds.size(), resultA.downstreamDependents().size(), "Downstream dependents must have no duplicates");
    }

    @Test
    @DisplayName("Test 11: Longer 3-node cycle A -> B -> C -> A terminates deterministically with no duplicates")
    void testLongerCycleThreeNodes() {
        TopologyNode n1 = TopologyNode.of(TopologyNodeType.SECURITY_GROUP, "sg-1", "351405419700", "ap-southeast-2", Map.of());
        TopologyNode n2 = TopologyNode.of(TopologyNodeType.SECURITY_GROUP, "sg-2", "351405419700", "ap-southeast-2", Map.of());
        TopologyNode n3 = TopologyNode.of(TopologyNodeType.SECURITY_GROUP, "sg-3", "351405419700", "ap-southeast-2", Map.of());

        // 1 -> 2 -> 3 -> 1
        TopologyEdge e12 = TopologyEdge.of(TopologyRelationshipType.EC2_ATTACHED_SECURITY_GROUP, n1.nodeId(), n2.nodeId(), "351405419700", "ap-southeast-2", Map.of());
        TopologyEdge e23 = TopologyEdge.of(TopologyRelationshipType.EC2_ATTACHED_SECURITY_GROUP, n2.nodeId(), n3.nodeId(), "351405419700", "ap-southeast-2", Map.of());
        TopologyEdge e31 = TopologyEdge.of(TopologyRelationshipType.EC2_ATTACHED_SECURITY_GROUP, n3.nodeId(), n1.nodeId(), "351405419700", "ap-southeast-2", Map.of());

        TopologyGraph graph = new TopologyGraph(
                "351405419700", "ap-southeast-2", Instant.now(), 3, 3,
                List.of(n1, n2, n3),
                List.of(e12, e23, e31)
        );
        when(topologyQueryService.getTopology(anyString())).thenReturn(graph);

        ImpactAnalysisResult result = engine.analyzeBlastRadius("SECURITY_GROUP", "sg-1", "ap-southeast-2", "351405419700", 10);

        assertEquals(ImpactAnalysisStatus.SUCCESS, result.status());
        assertEquals(2, result.totalAffectedResources()); // sg-2 and sg-3
        assertEquals(2, result.directAffectedCount());
        assertEquals(0, result.indirectAffectedCount());

        // Verify downstream: sg-3 -> sg-1 (sg-3 is direct downstream), sg-2 -> sg-3 (sg-2 is indirect downstream)
        assertEquals(2, result.downstreamDependents().size());
        assertEquals("sg-3", result.downstreamDependents().get(0).resourceId()); // depth 1
        assertEquals(1, result.downstreamDependents().get(0).minimumDepth());
        assertEquals("sg-2", result.downstreamDependents().get(1).resourceId()); // depth 2
        assertEquals(2, result.downstreamDependents().get(1).minimumDepth());

        // Verify upstream: sg-1 -> sg-2 (sg-2 is direct upstream), sg-2 -> sg-3 (sg-3 is indirect upstream)
        assertEquals(2, result.upstreamDependencies().size());
        assertEquals("sg-2", result.upstreamDependencies().get(0).resourceId()); // depth 1
        assertEquals(1, result.upstreamDependencies().get(0).minimumDepth());
        assertEquals("sg-3", result.upstreamDependencies().get(1).resourceId()); // depth 2
        assertEquals(2, result.upstreamDependencies().get(1).minimumDepth());

        // Verify no duplicate paths
        Set<ImpactPath> pathSet = new HashSet<>(result.impactPaths());
        assertEquals(pathSet.size(), result.impactPaths().size(), "Impact paths must be deduplicated");
    }
}
