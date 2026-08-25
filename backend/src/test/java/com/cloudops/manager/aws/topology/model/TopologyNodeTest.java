package com.cloudops.manager.aws.topology.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TopologyNodeTest {

    @Test
    @DisplayName("Should generate deterministic nodeId based on account, region, type, and id")
    void shouldGenerateDeterministicNodeId() {
        TopologyNode node = TopologyNode.of(
                TopologyNodeType.EC2_INSTANCE, "i-123", "123456789012", "us-east-1", Map.of("type", "t3.micro")
        );

        assertThat(node.nodeId()).isEqualTo("123456789012:us-east-1:EC2_INSTANCE:i-123");
        assertThat(node.resourceType()).isEqualTo(TopologyNodeType.EC2_INSTANCE);
        assertThat(node.resourceId()).isEqualTo("i-123");
    }
}