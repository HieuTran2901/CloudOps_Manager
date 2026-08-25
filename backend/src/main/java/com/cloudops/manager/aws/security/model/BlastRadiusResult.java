package com.cloudops.manager.aws.security.model;

import com.cloudops.manager.aws.topology.model.TopologyEdge;
import com.cloudops.manager.aws.topology.model.TopologyNode;

import java.util.List;

public record BlastRadiusResult(
    String sourceNodeId,
    int maxDepth,
    List<TopologyNode> reachableNodes,
    List<TopologyEdge> reachableEdges,
    int traversedNodeCount,
    int traversedEdgeCount,
    String accountId,
    String region
) {
    public BlastRadiusResult {
        reachableNodes = (reachableNodes != null) ? List.copyOf(reachableNodes) : List.of();
        reachableEdges = (reachableEdges != null) ? List.copyOf(reachableEdges) : List.of();
    }
}