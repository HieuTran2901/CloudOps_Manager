package com.cloudops.manager.aws.topology.controller;

import com.cloudops.manager.aws.sts.model.AwsAccountTarget;
import com.cloudops.manager.aws.topology.model.TopologyGraph;
import com.cloudops.manager.aws.topology.model.TopologyNode;
import com.cloudops.manager.aws.topology.model.TopologyPath;
import com.cloudops.manager.aws.topology.service.TopologyQueryService;
import com.cloudops.manager.common.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/aws/topology")
public class TopologyController {

    private final TopologyQueryService topologyService;

    public TopologyController(TopologyQueryService topologyService) {
        this.topologyService = topologyService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<TopologyGraph>> getTopology(
            @RequestParam(required = false) String region) {

        TopologyGraph graph = topologyService.getTopology(region);
        return ResponseEntity.ok(ApiResponse.success(graph, "Topology graph retrieved successfully."));
    }

    @GetMapping("/nodes/{nodeId}")
    public ResponseEntity<ApiResponse<TopologyNode>> getNode(
            @PathVariable String nodeId,
            @RequestParam(required = false) String region) {

        TopologyNode node = topologyService.findNode(nodeId, region)
                .orElseThrow(() -> new IllegalArgumentException("Topology node not found: " + nodeId));
        return ResponseEntity.ok(ApiResponse.success(node, "Topology node retrieved successfully."));
    }

    @GetMapping("/nodes/{nodeId}/neighbors")
    public ResponseEntity<ApiResponse<List<TopologyNode>>> getNeighbors(
            @PathVariable String nodeId,
            @RequestParam(required = false) String region) {

        List<TopologyNode> neighbors = topologyService.findNeighbors(nodeId, region);
        return ResponseEntity.ok(ApiResponse.success(neighbors, "Neighbors retrieved successfully."));
    }

    @GetMapping("/path")
    public ResponseEntity<ApiResponse<TopologyPath>> getPath(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) String region) {

        TopologyPath path = topologyService.findPath(from, to, region)
                .orElseThrow(() -> new IllegalArgumentException("No path found between " + from + " and " + to));
        return ResponseEntity.ok(ApiResponse.success(path, "Topology path retrieved successfully."));
    }

    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<ApiResponse<TopologyGraph>> getCrossAccountTopology(
            @PathVariable String accountId,
            @RequestParam String roleArn,
            @RequestParam(required = false) String roleSessionName,
            @RequestParam(required = false) String externalId,
            @RequestParam(required = false) String region) {

        AwsAccountTarget target = new AwsAccountTarget(accountId, roleArn, roleSessionName, externalId, region);
        TopologyGraph graph = topologyService.getCrossAccountTopology(target);
        return ResponseEntity.ok(ApiResponse.success(graph, "Cross-account topology graph retrieved successfully."));
    }
}