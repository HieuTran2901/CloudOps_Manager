package com.cloudops.manager.aws.topology.model;

import java.time.Instant;
import java.util.Map;

public record TopologyNode(
    String nodeId,
    TopologyNodeType resourceType,
    String resourceId,
    String accountId,
    String region,
    Map<String, Object> attributes,
    Instant discoveredAt
) implements Comparable<TopologyNode> {

    public TopologyNode {
        attributes = (attributes != null) ? Map.copyOf(attributes) : Map.of();
    }

    public static TopologyNode of(
            TopologyNodeType type,
            String resourceId,
            String accountId,
            String region,
            Map<String, Object> attributes) {

        String safeAcc = (accountId != null) ? accountId.trim() : "unknown";
        String safeReg = (region != null) ? region.trim() : "global";
        String safeId = (resourceId != null) ? resourceId.trim() : "unknown";
        String nodeId = safeAcc + ":" + safeReg + ":" + type.name() + ":" + safeId;

        return new TopologyNode(nodeId, type, safeId, safeAcc, safeReg, attributes, Instant.now());
    }

    @Override
    public int compareTo(TopologyNode other) {
        return this.nodeId.compareTo(other.nodeId);
    }
}