package com.cloudops.manager.aws.topology.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TopologyEdgeTest {

    @Test
    @DisplayName("Should generate deterministic edgeId based on type, source, and target")
    void shouldGenerateDeterministicEdgeId() {
        TopologyEdge edge = TopologyEdge.of(
                TopologyRelationshipType.EC2_IN_SUBNET,
                "123:us-east-1:EC2_INSTANCE:i-1",
                "123:us-east-1:SUBNET:sub-1",
                "123", "us-east-1", Map.of()
        );

        assertThat(edge.edgeId()).isEqualTo("EC2_IN_SUBNET:123:us-east-1:EC2_INSTANCE:i-1->123:us-east-1:SUBNET:sub-1");
    }
}