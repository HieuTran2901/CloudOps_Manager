package com.cloudops.manager.aws.dashboard.model;

import com.cloudops.manager.aws.compliance.model.ComplianceEvaluationReport;
import com.cloudops.manager.aws.cost.model.CostAggregationResult;
import com.cloudops.manager.aws.discovery.model.InventorySummary;
import com.cloudops.manager.aws.topology.model.TopologyGraph;

import java.time.Instant;

public record DashboardSnapshot(
        String accountId,
        String region,
        DashboardSnapshotStatus snapshotStatus,
        Instant generatedAt,
        Instant lastSuccessfulRefreshAt,
        SubsystemSnapshot<InventorySummary> resources,
        SubsystemSnapshot<TopologyGraph> topology,
        SubsystemSnapshot<ComplianceEvaluationReport> compliance,
        SubsystemSnapshot<CostAggregationResult> costs
) {
    public DashboardSnapshot withStatus(DashboardSnapshotStatus newStatus) {
        return new DashboardSnapshot(
                accountId,
                region,
                newStatus,
                generatedAt,
                lastSuccessfulRefreshAt,
                resources,
                topology,
                compliance,
                costs
        );
    }
}
