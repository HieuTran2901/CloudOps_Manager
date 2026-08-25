package com.cloudops.manager.aws.security.model;

import com.cloudops.manager.aws.topology.model.TopologyEdge;

import java.util.List;

public record SecurityPath(
    String sourceNodeId,
    String targetNodeId,
    List<String> nodeIds,
    List<TopologyEdge> edges,
    int length,
    String accountId,
    String region
) {
    public SecurityPath {
        nodeIds = (nodeIds != null) ? List.copyOf(nodeIds) : List.of();
        edges = (edges != null) ? List.copyOf(edges) : List.of();
    }
}