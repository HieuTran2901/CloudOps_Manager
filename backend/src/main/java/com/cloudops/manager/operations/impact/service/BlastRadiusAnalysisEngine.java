package com.cloudops.manager.operations.impact.service;

import com.cloudops.manager.aws.sts.model.CallerIdentity;
import com.cloudops.manager.aws.sts.service.AwsIdentityService;
import com.cloudops.manager.aws.topology.model.*;
import com.cloudops.manager.aws.topology.service.TopologyQueryService;
import com.cloudops.manager.operations.impact.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Deterministic Change Impact & Blast-Radius intelligence engine.
 * Computes upstream dependencies and downstream dependents over the canonical TopologyGraph.
 */
@Service("changeImpactBlastRadiusAnalysisEngine")
public class BlastRadiusAnalysisEngine {

    private static final Logger log = LoggerFactory.getLogger(BlastRadiusAnalysisEngine.class);

    private static final int DEFAULT_MAX_DEPTH = 3;
    private static final int MIN_DEPTH_CAP = 1;
    private static final int MAX_DEPTH_CAP = 10;
    private static final int MAX_TRAVERSAL_STEPS = 1000;

    private final TopologyQueryService topologyQueryService;
    private final TopologyResourceIdentityResolver identityResolver;
    private final AwsIdentityService identityService;
    private final String defaultRegion;

    public BlastRadiusAnalysisEngine(
            TopologyQueryService topologyQueryService,
            TopologyResourceIdentityResolver identityResolver,
            AwsIdentityService identityService,
            @Value("${cloudops.aws.region:ap-southeast-2}") String defaultRegion
    ) {
        this.topologyQueryService = topologyQueryService;
        this.identityResolver = identityResolver;
        this.identityService = identityService;
        this.defaultRegion = defaultRegion;
    }

    public ImpactAnalysisResult analyzeBlastRadius(
            String rawResourceType,
            String rawResourceId,
            String optionalRegion,
            String optionalAccountId,
            Integer optionalMaxDepth
    ) {
        String region = (optionalRegion != null && !optionalRegion.isBlank()) ? optionalRegion.trim() : defaultRegion;
        String accountId = resolveAccountId(optionalAccountId);
        int maxDepth = normalizeDepth(optionalMaxDepth);

        log.info("Analyzing blast radius for resource: {} ({}), account: {}, region: {}, maxDepth: {}",
                rawResourceId, rawResourceType, accountId, region, maxDepth);

        // 1. Validate Input
        if (rawResourceId == null || rawResourceId.isBlank()) {
            return ImpactAnalysisResult.empty(accountId, region, ImpactAnalysisStatus.INVALID_REQUEST,
                    "Parameter 'resourceId' must not be blank.");
        }

        if (identityResolver.isKnownUnsupportedType(rawResourceType)) {
            return ImpactAnalysisResult.empty(accountId, region, ImpactAnalysisStatus.UNSUPPORTED_RESOURCE_TYPE,
                    "Resource type '" + rawResourceType + "' is currently AVAILABLE_BUT_NOT_GRAPH_NODE and unsupported for graph traversal.");
        }

        Optional<TopologyNodeType> parsedTypeOpt = identityResolver.parseNodeType(rawResourceType);
        if (parsedTypeOpt.isEmpty()) {
            return ImpactAnalysisResult.empty(accountId, region, ImpactAnalysisStatus.INVALID_REQUEST,
                    "Unknown or unsupported resourceType: '" + rawResourceType + "'.");
        }

        TopologyNodeType nodeType = parsedTypeOpt.get();

        // 2. Fetch authoritative TopologyGraph
        TopologyGraph graph;
        try {
            graph = topologyQueryService.getTopology(region);
        } catch (Exception e) {
            log.warn("Failed to retrieve topology graph for region {}: {}", region, e.getMessage());
            return ImpactAnalysisResult.empty(accountId, region, ImpactAnalysisStatus.PARTIAL,
                    "Topology discovery failure: " + e.getMessage());
        }

        if (graph == null || graph.nodes().isEmpty()) {
            return ImpactAnalysisResult.empty(accountId, region, ImpactAnalysisStatus.NOT_FOUND,
                    "No topology nodes discovered in region " + region + ".");
        }

        // 3. Resolve Target Node
        Map<String, TopologyNode> nodeLookup = new HashMap<>();
        for (TopologyNode node : graph.nodes()) {
            nodeLookup.put(node.nodeId(), node);
        }

        NodeResolution resolution = resolveTargetNode(graph, nodeLookup, accountId, region, nodeType, rawResourceId.trim());
        if (resolution.status() != ImpactAnalysisStatus.SUCCESS) {
            return ImpactAnalysisResult.empty(accountId, region, resolution.status(), resolution.message());
        }

        TopologyNode targetNode = resolution.node();

        ImpactResourceSummary targetSummary = new ImpactResourceSummary(
                targetNode.nodeId(),
                targetNode.resourceType(),
                targetNode.resourceId(),
                targetNode.accountId(),
                targetNode.region(),
                0,
                false,
                targetNode.attributes()
        );

        // 4. Build Forward and Reverse Adjacency Maps
        // Outgoing: source -> List<edge> (Upstream dependencies: A -> B means A depends on B)
        Map<String, List<TopologyEdge>> outgoingAdj = new HashMap<>();
        // Incoming: target -> List<edge> (Downstream dependents: A -> B means A is dependent on B)
        Map<String, List<TopologyEdge>> incomingAdj = new HashMap<>();

        for (TopologyEdge edge : graph.edges()) {
            outgoingAdj.computeIfAbsent(edge.sourceNodeId(), k -> new ArrayList<>()).add(edge);
            incomingAdj.computeIfAbsent(edge.targetNodeId(), k -> new ArrayList<>()).add(edge);
        }

        // 5. Traverse Downstream Dependents (Blast Radius / Impact)
        List<ImpactPath> allPaths = new ArrayList<>();
        Map<String, Integer> downstreamMinDepths = new HashMap<>();
        traverseGraph(targetNode.nodeId(), incomingAdj, nodeLookup, maxDepth, downstreamMinDepths, allPaths, false);

        // 6. Traverse Upstream Dependencies (Root causes / Dependencies)
        Map<String, Integer> upstreamMinDepths = new HashMap<>();
        traverseGraph(targetNode.nodeId(), outgoingAdj, nodeLookup, maxDepth, upstreamMinDepths, allPaths, true);

        // 7. Aggregate Summaries
        List<ImpactResourceSummary> downstreamSummaries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : downstreamMinDepths.entrySet()) {
            String nId = entry.getKey();
            if (nId.equals(targetNode.nodeId())) continue;
            TopologyNode node = nodeLookup.get(nId);
            if (node != null) {
                int depth = entry.getValue();
                downstreamSummaries.add(new ImpactResourceSummary(
                        node.nodeId(), node.resourceType(), node.resourceId(),
                        node.accountId(), node.region(), depth, depth == 1, node.attributes()
                ));
            }
        }
        Collections.sort(downstreamSummaries);

        List<ImpactResourceSummary> upstreamSummaries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : upstreamMinDepths.entrySet()) {
            String nId = entry.getKey();
            if (nId.equals(targetNode.nodeId())) continue;
            TopologyNode node = nodeLookup.get(nId);
            if (node != null) {
                int depth = entry.getValue();
                upstreamSummaries.add(new ImpactResourceSummary(
                        node.nodeId(), node.resourceType(), node.resourceId(),
                        node.accountId(), node.region(), depth, depth == 1, node.attributes()
                ));
            }
        }
        Collections.sort(upstreamSummaries);

        // Calculate unique overall affected nodes (excluding target)
        Set<String> allAffectedNodeIds = new TreeSet<>();
        allAffectedNodeIds.addAll(downstreamMinDepths.keySet());
        allAffectedNodeIds.addAll(upstreamMinDepths.keySet());
        allAffectedNodeIds.remove(targetNode.nodeId());

        int directCount = 0;
        int indirectCount = 0;
        Map<String, Integer> typeSummary = new TreeMap<>();

        for (String affId : allAffectedNodeIds) {
            int dMin = downstreamMinDepths.getOrDefault(affId, Integer.MAX_VALUE);
            int uMin = upstreamMinDepths.getOrDefault(affId, Integer.MAX_VALUE);
            int minDepth = Math.min(dMin, uMin);

            if (minDepth == 1) {
                directCount++;
            } else {
                indirectCount++;
            }

            TopologyNode node = nodeLookup.get(affId);
            if (node != null) {
                typeSummary.merge(node.resourceType().name(), 1, Integer::sum);
            }
        }

        List<ImpactPath> uniquePaths = new ArrayList<>(new TreeSet<>(allPaths));

        List<String> warnings = new ArrayList<>();
        warnings.add("PUBLIC_PATH_ANALYSIS = NOT_SUPPORTED");

        return new ImpactAnalysisResult(
                targetSummary,
                accountId,
                region,
                maxDepth,
                allAffectedNodeIds.size(),
                directCount,
                indirectCount,
                typeSummary,
                upstreamSummaries,
                downstreamSummaries,
                uniquePaths,
                ImpactAnalysisStatus.SUCCESS,
                warnings,
                Instant.now()
        );
    }

    private void traverseGraph(
            String rootNodeId,
            Map<String, List<TopologyEdge>> adjacency,
            Map<String, TopologyNode> nodeLookup,
            int maxDepth,
            Map<String, Integer> minDepths,
            List<ImpactPath> collectedPaths,
            boolean isUpstream
    ) {
        Queue<TraversalStep> queue = new ArrayDeque<>();
        Set<String> visitedNodeIds = new HashSet<>();

        queue.add(new TraversalStep(rootNodeId, 0));
        visitedNodeIds.add(rootNodeId);
        minDepths.put(rootNodeId, 0);

        int stepCount = 0;
        while (!queue.isEmpty() && stepCount < MAX_TRAVERSAL_STEPS) {
            stepCount++;
            TraversalStep current = queue.poll();
            if (current.depth >= maxDepth) continue;

            List<TopologyEdge> edges = adjacency.getOrDefault(current.nodeId, List.of())
                    .stream()
                    .sorted()
                    .toList();

            for (TopologyEdge edge : edges) {
                String nextNodeId = isUpstream ? edge.targetNodeId() : edge.sourceNodeId();
                int nextDepth = current.depth + 1;

                // Record min depth
                minDepths.merge(nextNodeId, nextDepth, Math::min);

                // Record unique path
                collectedPaths.add(new ImpactPath(
                        edge.sourceNodeId(),
                        edge.targetNodeId(),
                        edge.relationshipType(),
                        nextDepth
                ));

                // Cycle-safe queue ingestion
                if (!visitedNodeIds.contains(nextNodeId)) {
                    visitedNodeIds.add(nextNodeId);
                    queue.add(new TraversalStep(nextNodeId, nextDepth));
                }
            }
        }
    }

    private NodeResolution resolveTargetNode(
            TopologyGraph graph,
            Map<String, TopologyNode> nodeLookup,
            String accountId,
            String region,
            TopologyNodeType type,
            String resourceId
    ) {
        // Fallback or exact matches by resourceType and case-insensitive resourceId
        List<TopologyNode> matches = graph.nodes().stream()
                .filter(n -> n.resourceType() == type && n.resourceId().equalsIgnoreCase(resourceId))
                .toList();

        if (matches.size() > 1) {
            return NodeResolution.ambiguous(
                    "Ambiguous target resource: found " + matches.size() + " resources of type " + type + " matching id '" + resourceId + "'."
            );
        }

        if (matches.size() == 1) {
            return NodeResolution.resolved(matches.get(0));
        }

        // Direct canonical key match
        String directKey = identityResolver.buildCanonicalNodeId(accountId, region, type, resourceId);
        if (nodeLookup.containsKey(directKey)) {
            return NodeResolution.resolved(nodeLookup.get(directKey));
        }

        if (nodeLookup.containsKey(resourceId)) {
            return NodeResolution.resolved(nodeLookup.get(resourceId));
        }

        return NodeResolution.notFound(
                "Target resource '" + resourceId + "' of type " + type + " was not found in topology graph."
        );
    }

    private int normalizeDepth(Integer depth) {
        if (depth == null) return DEFAULT_MAX_DEPTH;
        if (depth < MIN_DEPTH_CAP) return MIN_DEPTH_CAP;
        return Math.min(depth, MAX_DEPTH_CAP);
    }

    private String resolveAccountId(String optionalAccountId) {
        if (optionalAccountId != null && !optionalAccountId.isBlank()) {
            return optionalAccountId.trim();
        }
        try {
            CallerIdentity id = identityService.getCurrentIdentity();
            if (id != null && id.accountId() != null) {
                return id.accountId();
            }
        } catch (Exception ignored) {}
        return "351405419700";
    }

    private record TraversalStep(String nodeId, int depth) {}

    private record NodeResolution(TopologyNode node, ImpactAnalysisStatus status, String message) {
        static NodeResolution resolved(TopologyNode node) {
            return new NodeResolution(node, ImpactAnalysisStatus.SUCCESS, null);
        }
        static NodeResolution notFound(String message) {
            return new NodeResolution(null, ImpactAnalysisStatus.NOT_FOUND, message);
        }
        static NodeResolution ambiguous(String message) {
            return new NodeResolution(null, ImpactAnalysisStatus.AMBIGUOUS_RESOURCE, message);
        }
    }
}
