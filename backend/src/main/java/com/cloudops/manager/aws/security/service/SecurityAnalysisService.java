package com.cloudops.manager.aws.security.service;

import com.cloudops.manager.aws.discovery.model.*;
import com.cloudops.manager.aws.discovery.service.AwsResourceDiscoveryService;
import com.cloudops.manager.aws.security.engine.*;
import com.cloudops.manager.aws.security.model.*;
import com.cloudops.manager.aws.sts.model.AwsAccountTarget;
import com.cloudops.manager.aws.sts.service.AwsIdentityService;
import com.cloudops.manager.aws.topology.model.TopologyGraph;
import com.cloudops.manager.aws.topology.service.TopologyQueryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SecurityAnalysisService {

    private final TopologyQueryService topologyService;
    private final AwsResourceDiscoveryService discoveryService;
    private final AwsIdentityService identityService;
    private final BlastRadiusAnalysisEngine blastEngine;
    private final ReachabilityAnalysisEngine reachabilityEngine;
    private final ExposureAnalysisEngine exposureEngine;
    private final LateralMovementAnalysisEngine lateralEngine;

    @Value("${cloudops.aws.region:us-east-1}")
    private String defaultRegion;

    public SecurityAnalysisService(
            TopologyQueryService topologyService,
            AwsResourceDiscoveryService discoveryService,
            AwsIdentityService identityService,
            BlastRadiusAnalysisEngine blastEngine,
            ReachabilityAnalysisEngine reachabilityEngine,
            ExposureAnalysisEngine exposureEngine,
            LateralMovementAnalysisEngine lateralEngine) {
        this.topologyService = topologyService;
        this.discoveryService = discoveryService;
        this.identityService = identityService;
        this.blastEngine = blastEngine;
        this.reachabilityEngine = reachabilityEngine;
        this.exposureEngine = exposureEngine;
        this.lateralEngine = lateralEngine;
    }

    public BlastRadiusResult getBlastRadius(String nodeId, int maxDepth, String optionalRegion) {
        String region = resolveRegion(optionalRegion);
        TopologyGraph graph = topologyService.getTopology(region);
        return blastEngine.calculateBlastRadius(nodeId, maxDepth > 0 ? maxDepth : 3, graph);
    }

    public BlastRadiusResult getCrossAccountBlastRadius(AwsAccountTarget target, String nodeId, int maxDepth) {
        TopologyGraph graph = topologyService.getCrossAccountTopology(target);
        return blastEngine.calculateBlastRadius(nodeId, maxDepth > 0 ? maxDepth : 3, graph);
    }

    public SecurityReachabilityResult getReachability(String from, String to, int maxDepth, String optionalRegion) {
        String region = resolveRegion(optionalRegion);
        TopologyGraph graph = topologyService.getTopology(region);
        return reachabilityEngine.analyzeReachability(from, to, maxDepth > 0 ? maxDepth : 5, graph);
    }

    public List<SecurityExposureResult> getExposures(String optionalRegion) {
        String region = resolveRegion(optionalRegion);
        String accountId = identityService.getCurrentIdentity().accountId();

        InventorySummary summary = discoveryService.discoverAll(region);
        List<Ec2DetailResource> ec2List = new ArrayList<>();
        List<SecurityGroupDetailResource> sgList = new ArrayList<>();

        if (summary.resources() != null) {
            for (CloudResource r : summary.resources()) {
                if (r instanceof Ec2InstanceResource ec2) {
                    try { ec2List.add(discoveryService.getEc2InstanceDetail(ec2.resourceId(), region)); } catch (Exception ignored) {}
                } else if (r instanceof SecurityGroupResource sg) {
                    try { sgList.add(discoveryService.getSecurityGroupDetail(sg.resourceId(), region)); } catch (Exception ignored) {}
                }
            }
        }

        return exposureEngine.evaluateExposures(ec2List, sgList, accountId, region);
    }

    public List<LateralMovementResult> getLateralMovement(int maxDepth, String optionalRegion) {
        String region = resolveRegion(optionalRegion);
        List<SecurityExposureResult> exposures = getExposures(region);
        TopologyGraph graph = topologyService.getTopology(region);
        return lateralEngine.analyzePropagation(exposures, graph, maxDepth > 0 ? maxDepth : 3);
    }

    private String resolveRegion(String optionalRegion) {
        return (optionalRegion != null && !optionalRegion.isBlank()) ? optionalRegion.trim() : defaultRegion;
    }
}