package com.cloudops.manager.verification.isolation;

import com.cloudops.manager.aws.discovery.model.CloudResource;
import com.cloudops.manager.aws.discovery.model.InventorySummary;
import com.cloudops.manager.aws.security.engine.BlastRadiusAnalysisEngine;
import com.cloudops.manager.aws.security.model.BlastRadiusResult;
import com.cloudops.manager.aws.topology.model.TopologyGraph;
import com.cloudops.manager.aws.topology.model.TopologyNode;
import com.cloudops.manager.aws.topology.model.TopologyNodeType;
import com.cloudops.manager.verification.fixture.SyntheticEvidenceFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("22B.1 — Multi-Account Isolation Verification")
class MultiAccountIsolationTest {

    @Test
    @DisplayName("Verify identical resource IDs in different accounts produce globally unique, isolated node IDs")
    void testCrossAccountNodeCollisionSafety() {
        String accA = SyntheticEvidenceFixtures.SYNTHETIC_ACCOUNT_A;
        String accB = SyntheticEvidenceFixtures.SYNTHETIC_ACCOUNT_B;
        String region = SyntheticEvidenceFixtures.REGION_US_EAST_1;

        TopologyNode nodeA = SyntheticEvidenceFixtures.createSyntheticNode(accA, region, TopologyNodeType.EC2_INSTANCE, "i-123456");
        TopologyNode nodeB = SyntheticEvidenceFixtures.createSyntheticNode(accB, region, TopologyNodeType.EC2_INSTANCE, "i-123456");

        assertNotEquals(nodeA.nodeId(), nodeB.nodeId(), "Node IDs across different accounts must never collide");
        assertTrue(nodeA.nodeId().startsWith(accA));
        assertTrue(nodeB.nodeId().startsWith(accB));
        assertEquals("synthetic-i-123456", nodeA.resourceId());
        assertEquals("synthetic-i-123456", nodeB.resourceId());
    }

    @Test
    @DisplayName("Verify Blast Radius analysis strictly preserves account boundaries and does not leak cross-account")
    void testBlastRadiusAccountIsolation() {
        String accA = SyntheticEvidenceFixtures.SYNTHETIC_ACCOUNT_A;
        String accB = SyntheticEvidenceFixtures.SYNTHETIC_ACCOUNT_B;
        String region = SyntheticEvidenceFixtures.REGION_US_EAST_1;

        TopologyGraph graphA = SyntheticEvidenceFixtures.createSyntheticGraph(accA, region);
        TopologyGraph graphB = SyntheticEvidenceFixtures.createSyntheticGraph(accB, region);

        BlastRadiusAnalysisEngine engine = new BlastRadiusAnalysisEngine();
        String rootNodeA = graphA.nodes().get(2).nodeId();

        BlastRadiusResult resultA = engine.calculateBlastRadius(rootNodeA, 5, graphA);
        for (TopologyNode reachableNode : resultA.reachableNodes()) {
            assertTrue(reachableNode.nodeId().startsWith(accA), "Reachable nodes must strictly belong to Account A: " + reachableNode.nodeId());
            assertFalse(reachableNode.nodeId().startsWith(accB), "Account B node leaked into Account A blast radius!");
        }
    }

    @Test
    @DisplayName("Verify resource inventory filtering enforces account isolation")
    void testResourceInventoryIsolation() {
        String accA = SyntheticEvidenceFixtures.SYNTHETIC_ACCOUNT_A;
        String accB = SyntheticEvidenceFixtures.SYNTHETIC_ACCOUNT_B;
        String region = SyntheticEvidenceFixtures.REGION_US_EAST_1;

        InventorySummary invA = SyntheticEvidenceFixtures.createSyntheticInventory(accA, region, 5);
        InventorySummary invB = SyntheticEvidenceFixtures.createSyntheticInventory(accB, region, 5);

        for (CloudResource r : invA.resources()) {
            assertEquals(accA, r.accountId());
            assertNotEquals(accB, r.accountId());
        }
        for (CloudResource r : invB.resources()) {
            assertEquals(accB, r.accountId());
            assertNotEquals(accA, r.accountId());
        }
    }
}