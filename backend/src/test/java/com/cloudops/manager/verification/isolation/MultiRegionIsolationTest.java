package com.cloudops.manager.verification.isolation;

import com.cloudops.manager.aws.topology.model.TopologyNode;
import com.cloudops.manager.aws.topology.model.TopologyNodeType;
import com.cloudops.manager.verification.fixture.SyntheticEvidenceFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("22B.2 — Multi-Region Isolation Verification")
class MultiRegionIsolationTest {

    @Test
    @DisplayName("Verify resources across us-east-1, eu-west-1, and ap-southeast-2 remain isolated")
    void testMultiRegionScoping() {
        String accountId = SyntheticEvidenceFixtures.SYNTHETIC_ACCOUNT_A;

        TopologyNode nodeUs = SyntheticEvidenceFixtures.createSyntheticNode(accountId, SyntheticEvidenceFixtures.REGION_US_EAST_1, TopologyNodeType.VPC, "vpc-01");
        TopologyNode nodeEu = SyntheticEvidenceFixtures.createSyntheticNode(accountId, SyntheticEvidenceFixtures.REGION_EU_WEST_1, TopologyNodeType.VPC, "vpc-01");
        TopologyNode nodeAp = SyntheticEvidenceFixtures.createSyntheticNode(accountId, SyntheticEvidenceFixtures.REGION_AP_SOUTHEAST_2, TopologyNodeType.VPC, "vpc-01");

        assertNotEquals(nodeUs.nodeId(), nodeEu.nodeId());
        assertNotEquals(nodeEu.nodeId(), nodeAp.nodeId());
        assertNotEquals(nodeUs.nodeId(), nodeAp.nodeId());

        assertTrue(nodeUs.nodeId().contains(":us-east-1:"));
        assertTrue(nodeEu.nodeId().contains(":eu-west-1:"));
        assertTrue(nodeAp.nodeId().contains(":ap-southeast-2:"));
    }
}