package com.cloudops.manager.aws.topology.service;

import com.cloudops.manager.aws.discovery.model.*;
import com.cloudops.manager.aws.discovery.service.AwsResourceDiscoveryService;
import com.cloudops.manager.aws.sts.model.AwsAccountTarget;
import com.cloudops.manager.aws.sts.service.AwsIdentityService;
import com.cloudops.manager.aws.topology.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TopologyQueryService {

    private static final Logger log = LoggerFactory.getLogger(TopologyQueryService.class);

    private final TopologyGraphBuilder graphBuilder;
    private final AwsResourceDiscoveryService discoveryService;
    private final AwsIdentityService identityService;

    @Value("${cloudops.aws.region:us-east-1}")
    private String defaultRegion;

    public TopologyQueryService(
            TopologyGraphBuilder graphBuilder,
            AwsResourceDiscoveryService discoveryService,
            AwsIdentityService identityService) {
        this.graphBuilder = graphBuilder;
        this.discoveryService = discoveryService;
        this.identityService = identityService;
    }

    public TopologyGraph getTopology(String optionalRegion) {
        String region = resolveRegion(optionalRegion);
        String accountId = identityService.getCurrentIdentity().accountId();
        log.info("Generating topology graph for account: {}, region: {}", accountId, region);

        TopologyContext context = buildLocalContext(accountId, region);
        return graphBuilder.buildGraph(context);
    }

    public TopologyGraph getCrossAccountTopology(AwsAccountTarget target) {
        String region = resolveRegion(target.region());
        log.info("Generating cross-account topology for account: {}, region: {}", target.accountId(), region);

        InventorySummary summary = discoveryService.discoverAccount(target);
        TopologyContext context = new TopologyContext(
                target.accountId(), region, List.of(), List.of(), List.of(), List.of(), List.of()
        );
        return graphBuilder.buildGraph(context);
    }

    public Optional<TopologyNode> findNode(String nodeId, String optionalRegion) {
        TopologyGraph graph = getTopology(optionalRegion);
        return graph.nodes().stream().filter(n -> n.nodeId().equalsIgnoreCase(nodeId)).findFirst();
    }

    public List<TopologyNode> findNeighbors(String nodeId, String optionalRegion) {
        TopologyGraph graph = getTopology(optionalRegion);
        Set<String> neighborIds = new TreeSet<>();

        for (TopologyEdge edge : graph.edges()) {
            if (edge.sourceNodeId().equalsIgnoreCase(nodeId)) {
                neighborIds.add(edge.targetNodeId());
            } else if (edge.targetNodeId().equalsIgnoreCase(nodeId)) {
                neighborIds.add(edge.sourceNodeId());
            }
        }

        Map<String, TopologyNode> nodeMap = new HashMap<>();
        for (TopologyNode node : graph.nodes()) {
            nodeMap.put(node.nodeId(), node);
        }

        List<TopologyNode> neighbors = new ArrayList<>();
        for (String nId : neighborIds) {
            if (nodeMap.containsKey(nId)) {
                neighbors.add(nodeMap.get(nId));
            }
        }
        return neighbors.stream().sorted().toList();
    }

    public Optional<TopologyPath> findPath(String sourceNodeId, String targetNodeId, String optionalRegion) {
        if (sourceNodeId == null || targetNodeId == null) return Optional.empty();
        TopologyGraph graph = getTopology(optionalRegion);

        if (sourceNodeId.equalsIgnoreCase(targetNodeId)) {
            Optional<TopologyNode> nodeOpt = graph.nodes().stream().filter(n -> n.nodeId().equalsIgnoreCase(sourceNodeId)).findFirst();
            return nodeOpt.map(n -> new TopologyPath(sourceNodeId, targetNodeId, List.of(n), List.of(), 0));
        }

        Map<String, List<TopologyEdge>> adj = new HashMap<>();
        Map<String, TopologyNode> nodeMap = new HashMap<>();
        for (TopologyNode n : graph.nodes()) {
            nodeMap.put(n.nodeId(), n);
            adj.put(n.nodeId(), new ArrayList<>());
        }

        for (TopologyEdge edge : graph.edges()) {
            adj.computeIfAbsent(edge.sourceNodeId(), k -> new ArrayList<>()).add(edge);
        }

        // Deterministic BFS
        Queue<String> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        Map<String, TopologyEdge> edgeTo = new HashMap<>();
        Map<String, String> parentTo = new HashMap<>();

        queue.add(sourceNodeId);
        visited.add(sourceNodeId);

        boolean found = false;
        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (current.equalsIgnoreCase(targetNodeId)) {
                found = true;
                break;
            }

            List<TopologyEdge> outEdges = adj.getOrDefault(current, List.of()).stream().sorted().toList();
            for (TopologyEdge edge : outEdges) {
                String next = edge.targetNodeId();
                if (!visited.contains(next)) {
                    visited.add(next);
                    edgeTo.put(next, edge);
                    parentTo.put(next, current);
                    queue.add(next);
                }
            }
        }

        if (!found) return Optional.empty();

        List<TopologyNode> pathNodes = new ArrayList<>();
        List<TopologyEdge> pathEdges = new ArrayList<>();
        String curr = targetNodeId;

        while (!curr.equalsIgnoreCase(sourceNodeId)) {
            TopologyNode node = nodeMap.get(curr);
            if (node != null) pathNodes.add(0, node);
            TopologyEdge edge = edgeTo.get(curr);
            if (edge != null) pathEdges.add(0, edge);
            curr = parentTo.get(curr);
        }
        TopologyNode startNode = nodeMap.get(sourceNodeId);
        if (startNode != null) pathNodes.add(0, startNode);

        return Optional.of(new TopologyPath(sourceNodeId, targetNodeId, pathNodes, pathEdges, pathEdges.size()));
    }

    private TopologyContext buildLocalContext(String accountId, String region) {
        InventorySummary summary = discoveryService.discoverAll(region);
        List<Ec2DetailResource> ec2List = new ArrayList<>();
        List<SecurityGroupDetailResource> sgList = new ArrayList<>();
        List<RdsDetailResource> rdsList = new ArrayList<>();
        List<VpcTopologyResource> vpcList = new ArrayList<>();

        if (summary.resources() != null) {
            for (CloudResource r : summary.resources()) {
                if (r instanceof Ec2InstanceResource ec2) {
                    try { ec2List.add(discoveryService.getEc2InstanceDetail(ec2.resourceId(), region)); } catch (Exception ignored) {}
                } else if (r instanceof SecurityGroupResource sg) {
                    try { sgList.add(discoveryService.getSecurityGroupDetail(sg.resourceId(), region)); } catch (Exception ignored) {}
                } else if (r instanceof RdsInstanceResource rds) {
                    try { rdsList.add(discoveryService.getRdsInstanceDetail(rds.resourceId(), region)); } catch (Exception ignored) {}
                } else if (r instanceof VpcResource vpc) {
                    try { vpcList.add(discoveryService.getVpcTopology(vpc.resourceId(), region)); } catch (Exception ignored) {}
                }
            }
        }

        List<IamRoleResource> roles = List.of();
        try { roles = discoveryService.getIamRoles(); } catch (Exception ignored) {}

        return new TopologyContext(accountId, region, ec2List, sgList, vpcList, rdsList, roles);
    }

    private String resolveRegion(String optionalRegion) {
        return (optionalRegion != null && !optionalRegion.isBlank()) ? optionalRegion.trim() : defaultRegion;
    }
}