package com.cloudops.manager.aws.security.engine;

import com.cloudops.manager.aws.security.model.ReachabilityStatus;
import com.cloudops.manager.aws.security.model.SecurityReachabilityResult;
import com.cloudops.manager.aws.topology.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ReachabilityAnalysisEngineTest {

    private final ReachabilityAnalysisEngine engine = new ReachabilityAnalysisEngine();

    @Test
    @DisplayName("Should analyze reachability between two connected nodes")
    void shouldAnalyzeReachability() {
        TopologyNode ec2 = TopologyNode.of(TopologyNodeType.EC2_INSTANCE, "i-1", "123", "us-east-1", Map.of());
        TopologyNode subnet = TopologyNode.of(TopologyNodeType.SUBNET, "sub-1", "123", "us-east-1", Map.of());

        TopologyEdge edge = TopologyEdge.of(TopologyRelationshipType.EC2_IN_SUBNET, ec2.nodeId(), subnet.nodeId(), "123", "us-east-1", Map.of());
        TopologyGraph graph = new TopologyGraph("123", "us-east-1", Instant.now(), 2, 1, List.of(ec2, subnet), List.of(edge));

        SecurityReachabilityResult res = engine.analyzeReachability(ec2.nodeId(), subnet.nodeId(), 2, graph);
        assertThat(res.status()).isEqualTo(ReachabilityStatus.REACHABLE);
        assertThat(res.path()).isNotNull();
        assertThat(res.path().length()).isEqualTo(1);
    }
}