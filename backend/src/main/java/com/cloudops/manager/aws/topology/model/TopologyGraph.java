package com.cloudops.manager.aws.topology.model;

import java.time.Instant;
import java.util.List;

public record TopologyGraph(
    String accountId,
    String region,
    Instant generatedAt,
    int nodeCount,
    int edgeCount,
    List<TopologyNode> nodes,
    List<TopologyEdge> edges
) {
    public TopologyGraph {
        nodes = (nodes != null) ? List.copyOf(nodes) : List.of();
        edges = (edges != null) ? List.copyOf(edges) : List.of();
    }
}