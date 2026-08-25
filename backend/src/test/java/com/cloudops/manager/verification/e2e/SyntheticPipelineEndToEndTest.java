package com.cloudops.manager.verification.e2e;

import com.cloudops.manager.aws.discovery.model.InventorySummary;
import com.cloudops.manager.aws.forensics.export.ForensicIntegritySigner;
import com.cloudops.manager.aws.forensics.export.ForensicJsonExporter;
import com.cloudops.manager.aws.forensics.model.ForensicEvidenceItem;
import com.cloudops.manager.aws.forensics.model.ForensicExportResult;
import com.cloudops.manager.aws.security.engine.BlastRadiusAnalysisEngine;
import com.cloudops.manager.aws.security.engine.ReachabilityAnalysisEngine;
import com.cloudops.manager.aws.security.model.BlastRadiusResult;
import com.cloudops.manager.aws.security.model.ReachabilityStatus;
import com.cloudops.manager.aws.security.model.SecurityReachabilityResult;
import com.cloudops.manager.aws.topology.model.TopologyGraph;
import com.cloudops.manager.aws.topology.model.TopologyNode;
import com.cloudops.manager.verification.fixture.SyntheticEvidenceFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("22A.1 — Synthetic Pipeline End-to-End Verification")
class SyntheticPipelineEndToEndTest {

    @Test
    @DisplayName("Verify end-to-end evidence pipeline preserves identifier consistency across all layers")
    void testEndToEndPipelineConsistency() {
        String accountId = SyntheticEvidenceFixtures.SYNTHETIC_ACCOUNT_A;
        String region = SyntheticEvidenceFixtures.REGION_US_EAST_1;

        // 1. Discovery Evidence
        InventorySummary inventory = SyntheticEvidenceFixtures.createSyntheticInventory(accountId, region, 4);
        assertNotNull(inventory);
        assertEquals(4, inventory.totalCount());

        // 2. Topology Construction
        TopologyGraph graph = SyntheticEvidenceFixtures.createSyntheticGraph(accountId, region);
        assertNotNull(graph);
        assertEquals(4, graph.nodeCount());
        assertEquals(3, graph.edgeCount());

        for (TopologyNode node : graph.nodes()) {
            assertTrue(node.nodeId().startsWith(accountId + ":" + region + ":"),
                    "Node ID must be correctly scoped to account and region: " + node.nodeId());
            assertEquals(region, node.region());
        }

        // 3. Security Reachability & Blast Radius Analysis
        TopologyNode ec2Node = graph.nodes().get(2);
        TopologyNode vpcNode = graph.nodes().get(0);

        BlastRadiusAnalysisEngine blastEngine = new BlastRadiusAnalysisEngine();
        BlastRadiusResult blastResult = blastEngine.calculateBlastRadius(ec2Node.nodeId(), 3, graph);
        assertNotNull(blastResult);
        assertEquals(ec2Node.nodeId(), blastResult.sourceNodeId());
        assertTrue(blastResult.traversedNodeCount() >= 1);

        ReachabilityAnalysisEngine reachEngine = new ReachabilityAnalysisEngine();
        SecurityReachabilityResult reachResult = reachEngine.analyzeReachability(ec2Node.nodeId(), vpcNode.nodeId(), 5, graph);
        assertNotNull(reachResult);
        assertEquals(ReachabilityStatus.REACHABLE, reachResult.status());
        assertFalse(reachResult.path().nodeIds().isEmpty());
        assertEquals(ec2Node.nodeId(), reachResult.path().sourceNodeId());
        assertEquals(vpcNode.nodeId(), reachResult.path().targetNodeId());

        // 4. Forensic Evidence Export
        ForensicEvidenceItem ev1 = SyntheticEvidenceFixtures.createSyntheticEvidenceItem(accountId, region, "DISCOVERY", "001");
        ForensicEvidenceItem ev2 = SyntheticEvidenceFixtures.createSyntheticEvidenceItem(accountId, region, "TOPOLOGY", "002");
        List<ForensicEvidenceItem> evidenceList = List.of(ev1, ev2);

        ForensicIntegritySigner signer = new ForensicIntegritySigner();
        ForensicJsonExporter jsonExporter = new ForensicJsonExporter(signer);
        ForensicExportResult exportResult = jsonExporter.export(evidenceList, accountId, region);

        assertNotNull(exportResult);
        assertNotNull(exportResult.content());
        assertNotNull(exportResult.sha256Digest());
        assertEquals(64, exportResult.sha256Digest().length());
        assertTrue(exportResult.content().contains(accountId));
        assertTrue(exportResult.content().contains(region));
    }
}