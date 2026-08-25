package com.cloudops.manager.verification.isolation;

import com.cloudops.manager.aws.security.engine.BlastRadiusAnalysisEngine;
import com.cloudops.manager.aws.security.model.BlastRadiusResult;
import com.cloudops.manager.aws.topology.model.TopologyGraph;
import com.cloudops.manager.aws.topology.model.TopologyNode;
import com.cloudops.manager.aws.topology.model.TopologyNodeType;
import com.cloudops.manager.verification.fixture.SyntheticEvidenceFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Multi-Account Isolation Regression Gate")
class MultiAccountIsolationRegressionTest {

    private final BlastRadiusAnalysisEngine blastRadiusEngine = new BlastRadiusAnalysisEngine();

    @Test
    @DisplayName("Identical resource IDs across Account A and Account B produce isolated node IDs in blast radius")
    void testIdenticalResourceIdsIsolatedInBlastRadius() {
        String accA = SyntheticEvidenceFixtures.SYNTHETIC_ACCOUNT_A;
        String accB = SyntheticEvidenceFixtures.SYNTHETIC_ACCOUNT_B;
        String region = SyntheticEvidenceFixtures.REGION_US_EAST_1;

        TopologyGraph graphA = SyntheticEvidenceFixtures.createSyntheticGraph(accA, region);
        TopologyGraph graphB = SyntheticEvidenceFixtures.createSyntheticGraph(accB, region);

        String rootA = graphA.nodes().get(0).nodeId();
        String rootB = graphB.nodes().get(0).nodeId();

        BlastRadiusResult blastA = blastRadiusEngine.calculateBlastRadius(rootA, 3, graphA);
        BlastRadiusResult blastB = blastRadiusEngine.calculateBlastRadius(rootB, 3, graphB);

        assertNotEquals(blastA.sourceNodeId(), blastB.sourceNodeId());
        for (TopologyNode node : blastA.reachableNodes()) {
            assertTrue(node.nodeId().startsWith(accA));
            assertFalse(node.nodeId().startsWith(accB));
        }
    }
}