package com.cloudops.manager.operations.impact.model;

import com.cloudops.manager.aws.topology.model.TopologyNodeType;

import java.util.Map;

public record ImpactResourceSummary(
        String nodeId,
        TopologyNodeType resourceType,
        String resourceId,
        String accountId,
        String region,
        int minimumDepth,
        boolean isDirect,
        Map<String, Object> attributes
) implements Comparable<ImpactResourceSummary> {

    public ImpactResourceSummary {
        attributes = (attributes != null) ? Map.copyOf(attributes) : Map.of();
    }

    @Override
    public int compareTo(ImpactResourceSummary other) {
        int depthCmp = Integer.compare(this.minimumDepth, other.minimumDepth);
        if (depthCmp != 0) return depthCmp;
        return this.nodeId.compareTo(other.nodeId);
    }
}
