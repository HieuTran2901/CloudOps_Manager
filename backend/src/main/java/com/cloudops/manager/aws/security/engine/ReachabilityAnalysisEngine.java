package com.cloudops.manager.aws.security.engine;

import com.cloudops.manager.aws.security.model.ReachabilityStatus;
import com.cloudops.manager.aws.security.model.SecurityPath;
import com.cloudops.manager.aws.security.model.SecurityReachabilityResult;
import com.cloudops.manager.aws.topology.model.*;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ReachabilityAnalysisEngine {

    public SecurityReachabilityResult analyzeReachability(
            String sourceNodeId, String targetNodeId, int maxDepth, TopologyGraph graph) {

        if (sourceNodeId == null || targetNodeId == null || graph == null) {
            return new SecurityReachabilityResult(sourceNodeId, targetNodeId, ReachabilityStatus.INSUFFICIENT_EVIDENCE, null, maxDepth, "unknown", "unknown");
        }

        Map<String, TopologyNode> nodeMap = new HashMap<>();
        for (TopologyNode node : graph.nodes()) {
            nodeMap.put(node.nodeId(), node);
        }

        if (!nodeMap.containsKey(sourceNodeId) || !nodeMap.containsKey(targetNodeId)) {
            return new SecurityReachabilityResult(sourceNodeId, targetNodeId, ReachabilityStatus.NOT_REACHABLE, null, maxDepth, graph.accountId(), graph.region());
        }

        if (sourceNodeId.equalsIgnoreCase(targetNodeId)) {
            SecurityPath path = new SecurityPath(sourceNodeId, targetNodeId, List.of(sourceNodeId), List.of(), 0, graph.accountId(), graph.region());
            return new SecurityReachabilityResult(sourceNodeId, targetNodeId, ReachabilityStatus.REACHABLE, path, maxDepth, graph.accountId(), graph.region());
        }

        // Adjacency
        Map<String, List<TopologyEdge>> adj = new HashMap<>();
        for (TopologyEdge edge : graph.edges()) {
            adj.computeIfAbsent(edge.sourceNodeId(), k -> new ArrayList<>()).add(edge);
        }

        Queue<String> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        Map<String, Integer> depthMap = new HashMap<>();
        Map<String, TopologyEdge> edgeTo = new HashMap<>();
        Map<String, String> parentTo = new HashMap<>();

        queue.add(sourceNodeId);
        visited.add(sourceNodeId);
        depthMap.put(sourceNodeId, 0);

        boolean found = false;
        while (!queue.isEmpty()) {
            String current = queue.poll();
            int currentDepth = depthMap.get(current);

            if (current.equalsIgnoreCase(targetNodeId)) {
                found = true;
                break;
            }

            if (currentDepth >= maxDepth) continue;

            List<TopologyEdge> edges = adj.getOrDefault(current, List.of()).stream().sorted().toList();
            for (TopologyEdge edge : edges) {
                String next = edge.targetNodeId();
                if (!visited.contains(next)) {
                    visited.add(next);
                    depthMap.put(next, currentDepth + 1);
                    edgeTo.put(next, edge);
                    parentTo.put(next, current);
                    queue.add(next);
                }
            }
        }

        if (!found) {
            return new SecurityReachabilityResult(sourceNodeId, targetNodeId, ReachabilityStatus.NOT_REACHABLE, null, maxDepth, graph.accountId(), graph.region());
        }

        List<String> pathNodeIds = new ArrayList<>();
        List<TopologyEdge> pathEdges = new ArrayList<>();
        String curr = targetNodeId;

        while (!curr.equalsIgnoreCase(sourceNodeId)) {
            pathNodeIds.add(0, curr);
            TopologyEdge edge = edgeTo.get(curr);
            if (edge != null) pathEdges.add(0, edge);
            curr = parentTo.get(curr);
        }
        pathNodeIds.add(0, sourceNodeId);

        SecurityPath path = new SecurityPath(sourceNodeId, targetNodeId, pathNodeIds, pathEdges, pathEdges.size(), graph.accountId(), graph.region());
        return new SecurityReachabilityResult(sourceNodeId, targetNodeId, ReachabilityStatus.REACHABLE, path, maxDepth, graph.accountId(), graph.region());
    }
}