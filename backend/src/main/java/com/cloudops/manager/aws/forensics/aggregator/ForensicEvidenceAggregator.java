package com.cloudops.manager.aws.forensics.aggregator;

import com.cloudops.manager.aws.compliance.model.ComplianceEvaluationReport;
import com.cloudops.manager.aws.compliance.model.ComplianceEvaluationResult;
import com.cloudops.manager.aws.compliance.service.ComplianceEvaluationService;
import com.cloudops.manager.aws.discovery.model.*;
import com.cloudops.manager.aws.discovery.service.AwsResourceDiscoveryService;
import com.cloudops.manager.aws.forensics.model.ForensicEvidenceItem;
import com.cloudops.manager.aws.security.model.SecurityExposureResult;
import com.cloudops.manager.aws.security.service.SecurityAnalysisService;
import com.cloudops.manager.aws.topology.model.TopologyEdge;
import com.cloudops.manager.aws.topology.model.TopologyGraph;
import com.cloudops.manager.aws.topology.model.TopologyNode;
import com.cloudops.manager.aws.topology.service.TopologyQueryService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ForensicEvidenceAggregator {

    private final AwsResourceDiscoveryService discoveryService;
    private final ComplianceEvaluationService complianceService;
    private final TopologyQueryService topologyService;
    private final SecurityAnalysisService securityService;

    public ForensicEvidenceAggregator(
            AwsResourceDiscoveryService discoveryService,
            ComplianceEvaluationService complianceService,
            TopologyQueryService topologyService,
            SecurityAnalysisService securityService) {
        this.discoveryService = discoveryService;
        this.complianceService = complianceService;
        this.topologyService = topologyService;
        this.securityService = securityService;
    }

    public List<ForensicEvidenceItem> aggregate(String accountId, String region) {
        List<ForensicEvidenceItem> items = new ArrayList<>();

        // 1. Discovery Evidence
        try {
            InventorySummary summary = discoveryService.discoverAll(region);
            if (summary != null && summary.resources() != null) {
                for (CloudResource r : summary.resources()) {
                    items.add(new ForensicEvidenceItem(
                            "DISCOVERY",
                            r.resourceType() != null ? r.resourceType().name() : "UNKNOWN",
                            r.resourceId() != null ? r.resourceId() : "unknown",
                            accountId,
                            region,
                            "AwsResourceDiscoveryService",
                            Map.of("name", r.name() != null ? r.name() : "", "status", r.status() != null ? r.status() : "")
                    ));
                }
            }
        } catch (Exception ignored) {}

        // 2. Compliance Evaluation Evidence
        try {
            ComplianceEvaluationReport report = complianceService.evaluateLocal(region, List.of());
            if (report != null && report.results() != null) {
                for (ComplianceEvaluationResult res : report.results()) {
                    items.add(new ForensicEvidenceItem(
                            "COMPLIANCE",
                            "COMPLIANCE_RULE",
                            res.ruleId(),
                            accountId,
                            region,
                            "ComplianceEvaluationService",
                            Map.of("ruleId", res.ruleId(), "status", res.status().name(), "category", res.category() != null ? res.category().name() : "")
                    ));
                }
            }
        } catch (Exception ignored) {}

        // 3. Topology Evidence
        try {
            TopologyGraph graph = topologyService.getTopology(region);
            if (graph != null) {
                if (graph.nodes() != null) {
                    for (TopologyNode node : graph.nodes()) {
                        items.add(new ForensicEvidenceItem(
                                "TOPOLOGY_NODE",
                                node.resourceType() != null ? node.resourceType().name() : "NODE",
                                node.resourceId() != null ? node.resourceId() : node.nodeId(),
                                accountId,
                                region,
                                "TopologyQueryService",
                                Map.of("nodeId", node.nodeId())
                        ));
                    }
                }
                if (graph.edges() != null) {
                    for (TopologyEdge edge : graph.edges()) {
                        items.add(new ForensicEvidenceItem(
                                "TOPOLOGY_EDGE",
                                edge.relationshipType() != null ? edge.relationshipType().name() : "EDGE",
                                edge.edgeId(),
                                accountId,
                                region,
                                "TopologyQueryService",
                                Map.of("sourceNodeId", edge.sourceNodeId(), "targetNodeId", edge.targetNodeId())
                        ));
                    }
                }
            }
        } catch (Exception ignored) {}

        // 4. Security Exposure Evidence
        try {
            List<SecurityExposureResult> exposures = securityService.getExposures(region);
            if (exposures != null) {
                for (SecurityExposureResult exp : exposures) {
                    items.add(new ForensicEvidenceItem(
                            "SECURITY_EXPOSURE",
                            exp.resourceType() != null ? exp.resourceType() : "EXPOSURE",
                            exp.resourceId() != null ? exp.resourceId() : exp.nodeId(),
                            accountId,
                            region,
                            "SecurityAnalysisService",
                            Map.of("nodeId", exp.nodeId(), "status", exp.status().name())
                    ));
                }
            }
        } catch (Exception ignored) {}

        return items.stream().sorted().toList();
    }
}