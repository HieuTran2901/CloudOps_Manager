package com.cloudops.manager.operations.impact.model;

import com.cloudops.manager.aws.topology.model.TopologyNodeType;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ImpactAnalysisResult(
        ImpactResourceSummary targetResource,
        String accountId,
        String region,
        int maxDepth,
        int totalAffectedResources,
        int directAffectedCount,
        int indirectAffectedCount,
        Map<String, Integer> affectedTypeSummary,
        List<ImpactResourceSummary> upstreamDependencies,
        List<ImpactResourceSummary> downstreamDependents,
        List<ImpactPath> impactPaths,
        ImpactAnalysisStatus status,
        List<String> warnings,
        Instant evaluatedAt
) {
    public ImpactAnalysisResult {
        affectedTypeSummary = (affectedTypeSummary != null) ? Map.copyOf(affectedTypeSummary) : Map.of();
        upstreamDependencies = (upstreamDependencies != null) ? List.copyOf(upstreamDependencies) : List.of();
        downstreamDependents = (downstreamDependents != null) ? List.copyOf(downstreamDependents) : List.of();
        impactPaths = (impactPaths != null) ? List.copyOf(impactPaths) : List.of();
        warnings = (warnings != null) ? List.copyOf(warnings) : List.of();
    }

    public static ImpactAnalysisResult empty(String accountId, String region, ImpactAnalysisStatus status, String warningMessage) {
        return new ImpactAnalysisResult(
                null,
                accountId != null ? accountId : "unknown",
                region != null ? region : "unknown",
                0,
                0,
                0,
                0,
                Map.of(),
                List.of(),
                List.of(),
                List.of(),
                status,
                warningMessage != null ? List.of(warningMessage) : List.of(),
                Instant.now()
        );
    }
}
