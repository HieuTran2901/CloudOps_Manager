package com.cloudops.manager.aws.topology.model;

import java.util.List;

public record TopologyPath(
    String sourceNodeId,
    String targetNodeId,
    List<TopologyNode> nodes,
    List<TopologyEdge> edges,
    int length
) {
    public TopologyPath {
        nodes = (nodes != null) ? List.copyOf(nodes) : List.of();
        edges = (edges != null) ? List.copyOf(edges) : List.of();
    }
}