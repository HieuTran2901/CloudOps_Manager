package com.cloudops.manager.aws.security.engine;

import com.cloudops.manager.aws.security.model.BlastRadiusResult;
import com.cloudops.manager.aws.topology.model.*;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class BlastRadiusAnalysisEngine {

    public BlastRadiusResult calculateBlastRadius(String sourceNodeId, int maxDepth, TopologyGraph graph) {
        if (sourceNodeId == null || graph == null) {
            return new BlastRadiusResult(sourceNodeId, maxDepth, List.of(), List.of(), 0, 0, "unknown", "unknown");
        }

        Map<String, TopologyNode> nodeMap = new HashMap<>();
        for (TopologyNode node : graph.nodes()) {
            nodeMap.put(node.nodeId(), node);
        }

        TopologyNode sourceNode = nodeMap.get(sourceNodeId);
        if (sourceNode == null) {
            return new BlastRadiusResult(sourceNodeId, maxDepth, List.of(), List.of(), 0, 0, graph.accountId(), graph.region());
        }

        if (maxDepth <= 0) {
            return new BlastRadiusResult(sourceNodeId, 0, List.of(sourceNode), List.of(), 1, 0, graph.accountId(), graph.region());
        }

        // Build adjacency map
        Map<String, List<TopologyEdge>> adj = new HashMap<>();
        for (TopologyEdge edge : graph.edges()) {
            adj.computeIfAbsent(edge.sourceNodeId(), k -> new ArrayList<>()).add(edge);
            adj.computeIfAbsent(edge.targetNodeId(), k -> new ArrayList<>()).add(edge);
        }

        // BFS with depth tracking
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new ArrayDeque<>();
        Map<String, Integer> depthMap = new HashMap<>();
        Set<TopologyEdge> traversedEdges = new HashSet<>();

        visited.add(sourceNodeId);
        queue.add(sourceNodeId);
        depthMap.put(sourceNodeId, 0);

        List<TopologyNode> reachableNodes = new ArrayList<>();
        reachableNodes.add(sourceNode);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            int currentDepth = depthMap.get(current);

            if (currentDepth >= maxDepth) continue;

            List<TopologyEdge> edges = adj.getOrDefault(current, List.of()).stream().sorted().toList();
            for (TopologyEdge edge : edges) {
                String next = edge.sourceNodeId().equalsIgnoreCase(current) ? edge.targetNodeId() : edge.sourceNodeId();
                traversedEdges.add(edge);

                if (!visited.contains(next)) {
                    visited.add(next);
                    depthMap.put(next, currentDepth + 1);
                    queue.add(next);
                    TopologyNode nextNode = nodeMap.get(next);
                    if (nextNode != null) {
                        reachableNodes.add(nextNode);
                    }
                }
            }
        }

        List<TopologyNode> sortedNodes = reachableNodes.stream().sorted().toList();
        List<TopologyEdge> sortedEdges = traversedEdges.stream().sorted().toList();

        return new BlastRadiusResult(
                sourceNodeId,
                maxDepth,
                sortedNodes,
                sortedEdges,
                sortedNodes.size(),
                sortedEdges.size(),
                graph.accountId(),
                graph.region()
        );
    }
}