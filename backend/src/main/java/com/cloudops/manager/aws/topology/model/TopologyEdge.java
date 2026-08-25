package com.cloudops.manager.aws.topology.model;

import java.util.Map;

public record TopologyEdge(
    String edgeId,
    String sourceNodeId,
    String targetNodeId,
    TopologyRelationshipType relationshipType,
    String accountId,
    String region,
    Map<String, Object> evidence
) implements Comparable<TopologyEdge> {

    public TopologyEdge {
        evidence = (evidence != null) ? Map.copyOf(evidence) : Map.of();
    }

    public static TopologyEdge of(
            TopologyRelationshipType type,
            String sourceNodeId,
            String targetNodeId,
            String accountId,
            String region,
            Map<String, Object> evidence) {

        String edgeId = type.name() + ":" + sourceNodeId + "->" + targetNodeId;
        return new TopologyEdge(edgeId, sourceNodeId, targetNodeId, type, accountId, region, evidence);
    }

    @Override
    public int compareTo(TopologyEdge other) {
        return this.edgeId.compareTo(other.edgeId);
    }
}