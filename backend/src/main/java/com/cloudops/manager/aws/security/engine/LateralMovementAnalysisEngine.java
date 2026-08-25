package com.cloudops.manager.aws.security.engine;

import com.cloudops.manager.aws.security.model.ExposureStatus;
import com.cloudops.manager.aws.security.model.LateralMovementResult;
import com.cloudops.manager.aws.security.model.ReachabilityStatus;
import com.cloudops.manager.aws.security.model.SecurityExposureResult;
import com.cloudops.manager.aws.security.model.SecurityPath;
import com.cloudops.manager.aws.topology.model.*;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class LateralMovementAnalysisEngine {

    private final BlastRadiusAnalysisEngine blastEngine;

    public LateralMovementAnalysisEngine(BlastRadiusAnalysisEngine blastEngine) {
        this.blastEngine = blastEngine;
    }

    public List<LateralMovementResult> analyzePropagation(
            List<SecurityExposureResult> exposures,
            TopologyGraph graph,
            int maxDepth) {

        if (exposures == null || graph == null) return List.of();

        List<LateralMovementResult> results = new ArrayList<>();

        for (SecurityExposureResult exp : exposures) {
            if (exp.status() == ExposureStatus.EXPOSED) {
                var blast = blastEngine.calculateBlastRadius(exp.nodeId(), maxDepth, graph);

                for (TopologyNode reachable : blast.reachableNodes()) {
                    if (!reachable.nodeId().equalsIgnoreCase(exp.nodeId())) {
                        results.add(new LateralMovementResult(
                                exp.nodeId(),
                                reachable.nodeId(),
                                ReachabilityStatus.REACHABLE,
                                new SecurityPath(exp.nodeId(), reachable.nodeId(), List.of(exp.nodeId(), reachable.nodeId()), List.of(), 1, graph.accountId(), graph.region()),
                                Map.of("exposedSource", exp.nodeId(), "reachableTarget", reachable.nodeId()),
                                graph.accountId(),
                                graph.region()
                        ));
                    }
                }
            }
        }

        return results.stream().sorted(Comparator.comparing(LateralMovementResult::targetNodeId)).toList();
    }
}