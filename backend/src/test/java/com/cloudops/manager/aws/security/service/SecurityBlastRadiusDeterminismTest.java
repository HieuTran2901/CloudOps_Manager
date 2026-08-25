package com.cloudops.manager.aws.security.service;

import com.cloudops.manager.aws.security.engine.BlastRadiusAnalysisEngine;
import com.cloudops.manager.aws.security.model.BlastRadiusResult;
import com.cloudops.manager.aws.topology.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityBlastRadiusDeterminismTest {

    @Test
    @DisplayName("Evaluating blast radius repeatedly on identical graph must produce identical node ordering")
    void shouldProduceDeterministicBlastRadius() {
        BlastRadiusAnalysisEngine engine = new BlastRadiusAnalysisEngine();

        TopologyNode ec2 = TopologyNode.of(TopologyNodeType.EC2_INSTANCE, "i-1", "123", "us-east-1", Map.of());
        TopologyNode subnet = TopologyNode.of(TopologyNodeType.SUBNET, "sub-1", "123", "us-east-1", Map.of());
        TopologyNode vpc = TopologyNode.of(TopologyNodeType.VPC, "vpc-1", "123", "us-east-1", Map.of());

        TopologyEdge e1 = TopologyEdge.of(TopologyRelationshipType.EC2_IN_SUBNET, ec2.nodeId(), subnet.nodeId(), "123", "us-east-1", Map.of());
        TopologyEdge e2 = TopologyEdge.of(TopologyRelationshipType.SUBNET_IN_VPC, subnet.nodeId(), vpc.nodeId(), "123", "us-east-1", Map.of());

        TopologyGraph graph = new TopologyGraph("123", "us-east-1", Instant.now(), 3, 2, List.of(ec2, subnet, vpc), List.of(e1, e2));

        BlastRadiusResult r1 = engine.calculateBlastRadius(ec2.nodeId(), 2, graph);
        BlastRadiusResult r2 = engine.calculateBlastRadius(ec2.nodeId(), 2, graph);

        assertThat(r1.traversedNodeCount()).isEqualTo(r2.traversedNodeCount());
        for (int i = 0; i < r1.traversedNodeCount(); i++) {
            assertThat(r1.reachableNodes().get(i).nodeId()).isEqualTo(r2.reachableNodes().get(i).nodeId());
        }
    }
}