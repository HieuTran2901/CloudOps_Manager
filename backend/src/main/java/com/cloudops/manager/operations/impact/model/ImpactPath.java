package com.cloudops.manager.operations.impact.model;

import com.cloudops.manager.aws.topology.model.TopologyRelationshipType;

public record ImpactPath(
        String sourceNodeId,
        String targetNodeId,
        TopologyRelationshipType relationshipType,
        int depth
) implements Comparable<ImpactPath> {

    @Override
    public int compareTo(ImpactPath other) {
        int depthCmp = Integer.compare(this.depth, other.depth);
        if (depthCmp != 0) return depthCmp;
        int srcCmp = this.sourceNodeId.compareTo(other.sourceNodeId);
        if (srcCmp != 0) return srcCmp;
        int tgtCmp = this.targetNodeId.compareTo(other.targetNodeId);
        if (tgtCmp != 0) return tgtCmp;
        return this.relationshipType.name().compareTo(other.relationshipType.name());
    }
}
