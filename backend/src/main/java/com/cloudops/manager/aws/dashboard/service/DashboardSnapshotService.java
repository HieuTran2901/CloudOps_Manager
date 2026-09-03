package com.cloudops.manager.aws.dashboard.service;

import com.cloudops.manager.aws.compliance.model.ComplianceEvaluationReport;
import com.cloudops.manager.aws.compliance.service.ComplianceEvaluationService;
import com.cloudops.manager.aws.cost.model.CostAggregationResult;
import com.cloudops.manager.aws.cost.service.CostObservabilityService;
import com.cloudops.manager.aws.dashboard.cache.DashboardRefreshLock;
import com.cloudops.manager.aws.dashboard.cache.DashboardSnapshotCache;
import com.cloudops.manager.aws.dashboard.model.*;
import com.cloudops.manager.aws.discovery.model.InventorySummary;
import com.cloudops.manager.aws.discovery.service.AwsResourceDiscoveryService;
import com.cloudops.manager.aws.sts.service.AwsIdentityService;
import com.cloudops.manager.aws.topology.model.TopologyGraph;
import com.cloudops.manager.aws.topology.service.TopologyQueryService;
import com.cloudops.manager.common.exception.AwsAccessDeniedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class DashboardSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(DashboardSnapshotService.class);

    private final AwsResourceDiscoveryService discoveryService;
    private final TopologyQueryService topologyQueryService;
    private final ComplianceEvaluationService complianceService;
    private final CostObservabilityService costService;
    private final AwsIdentityService awsIdentityService;
    private final DashboardSnapshotCache snapshotCache;
    private final DashboardRefreshLock refreshLock;

    @Value("${cloudops.aws.region:us-east-1}")
    private String defaultRegion;

    @Value("${cloudops.dashboard.snapshot.fresh-ttl-seconds:60}")
    private long freshTtlSeconds = 60;

    @Value("${cloudops.dashboard.snapshot.stale-ttl-seconds:600}")
    private long staleTtlSeconds = 600;

    @Value("${cloudops.dashboard.snapshot.refresh-lock-ttl-seconds:120}")
    private long refreshLockTtlSeconds = 120;

    public DashboardSnapshotService(
            AwsResourceDiscoveryService discoveryService,
            TopologyQueryService topologyQueryService,
            ComplianceEvaluationService complianceService,
            CostObservabilityService costService,
            AwsIdentityService awsIdentityService,
            DashboardSnapshotCache snapshotCache,
            DashboardRefreshLock refreshLock) {
        this.discoveryService = discoveryService;
        this.topologyQueryService = topologyQueryService;
        this.complianceService = complianceService;
        this.costService = costService;
        this.awsIdentityService = awsIdentityService;
        this.snapshotCache = snapshotCache;
        this.refreshLock = refreshLock;
    }

    public DashboardSnapshot getSnapshot(String optionalRegion) {
        String region = resolveRegion(optionalRegion);
        String accountId = resolveAccountId();

        Optional<DashboardSnapshot> cachedOpt = snapshotCache.get(accountId, region);

        if (cachedOpt.isPresent()) {
            DashboardSnapshot cached = cachedOpt.get();
            long ageSeconds = Duration.between(cached.generatedAt(), Instant.now()).getSeconds();

            if (ageSeconds <= freshTtlSeconds) {
                log.info("DASHBOARD_SNAPSHOT_CACHE_HIT (FRESH, age: {}s) for region: {}", ageSeconds, region);
                return cached.withStatus(DashboardSnapshotStatus.LIVE);
            } else if (ageSeconds <= staleTtlSeconds) {
                log.info("DASHBOARD_SNAPSHOT_STALE_SERVE (STALE, age: {}s) for region: {}. Triggering background refresh.", ageSeconds, region);
                triggerBackgroundRefresh(accountId, region);
                return cached.withStatus(DashboardSnapshotStatus.STALE);
            } else {
                log.info("DASHBOARD_SNAPSHOT_EXPIRED (age: {}s) for region: {}. Synchronous refresh required.", ageSeconds, region);
                return refreshSynchronouslyOrReturnStale(accountId, region, cached);
            }
        }

        log.info("DASHBOARD_SNAPSHOT_CACHE_MISS for region: {}. Performing single-flight initial ingestion.", region);
        if (refreshLock.tryLock(accountId, region, refreshLockTtlSeconds)) {
            try {
                return buildAndCacheSnapshot(accountId, region);
            } finally {
                refreshLock.unlock(accountId, region);
            }
        } else {
            log.info("Initial ingestion lock held by another thread/node for region: {}. Awaiting cached snapshot.", region);
            for (int i = 0; i < 30; i++) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                Optional<DashboardSnapshot> retry = snapshotCache.get(accountId, region);
                if (retry.isPresent()) {
                    log.info("Retrieved snapshot populated by lock holder for region: {}", region);
                    return retry.get().withStatus(DashboardSnapshotStatus.LIVE);
                }
            }
            return buildAndCacheSnapshot(accountId, region);
        }
    }

    public DashboardSnapshot refreshSnapshot(String optionalRegion) {
        String region = resolveRegion(optionalRegion);
        String accountId = resolveAccountId();
        return buildAndCacheSnapshot(accountId, region);
    }

    private DashboardSnapshot refreshSynchronouslyOrReturnStale(String accountId, String region, DashboardSnapshot staleSnapshot) {
        if (refreshLock.tryLock(accountId, region, refreshLockTtlSeconds)) {
            try {
                return buildAndCacheSnapshot(accountId, region);
            } catch (Exception e) {
                log.error("DASHBOARD_SNAPSHOT_REFRESH_FAILURE during synchronous refresh for region: {}", region, e);
                return staleSnapshot.withStatus(DashboardSnapshotStatus.REFRESH_FAILED);
            } finally {
                refreshLock.unlock(accountId, region);
            }
        } else {
            return staleSnapshot.withStatus(DashboardSnapshotStatus.STALE);
        }
    }

    private void triggerBackgroundRefresh(String accountId, String region) {
        if (refreshLock.tryLock(accountId, region, refreshLockTtlSeconds)) {
            log.info("DASHBOARD_SNAPSHOT_REFRESH_START in background for region: {}", region);
            CompletableFuture.runAsync(() -> {
                try {
                    buildAndCacheSnapshot(accountId, region);
                    log.info("DASHBOARD_SNAPSHOT_REFRESH_SUCCESS in background for region: {}", region);
                } catch (Exception e) {
                    log.error("DASHBOARD_SNAPSHOT_REFRESH_FAILURE in background for region: {}", region, e);
                } finally {
                    refreshLock.unlock(accountId, region);
                }
            });
        }
    }

    public DashboardSnapshot buildAndCacheSnapshot(String accountId, String region) {
        Instant startTime = Instant.now();
        log.info("Building fresh DashboardSnapshot for account: {}, region: {}", accountId, region);

        SubsystemSnapshot<InventorySummary> resourcesSubsystem = fetchResourcesSubsystem(region);
        SubsystemSnapshot<TopologyGraph> topologySubsystem = fetchTopologySubsystem(region);
        SubsystemSnapshot<ComplianceEvaluationReport> complianceSubsystem = fetchComplianceSubsystem(region);
        SubsystemSnapshot<CostAggregationResult> costsSubsystem = fetchCostsSubsystem();

        DashboardSnapshot snapshot = new DashboardSnapshot(
                accountId,
                region,
                DashboardSnapshotStatus.LIVE,
                startTime,
                startTime,
                resourcesSubsystem,
                topologySubsystem,
                complianceSubsystem,
                costsSubsystem
        );

        snapshotCache.put(snapshot);
        log.info("DASHBOARD_SNAPSHOT_ATOMIC_REPLACE complete for region: {} in {}ms", region, Duration.between(startTime, Instant.now()).toMillis());
        return snapshot;
    }

    private SubsystemSnapshot<InventorySummary> fetchResourcesSubsystem(String region) {
        try {
            InventorySummary summary = discoveryService.discoverAll(region);
            if (summary.totalCount() == 0) {
                return SubsystemSnapshot.empty("AWS_RESOURCE_DISCOVERY", summary);
            }
            return SubsystemSnapshot.live("AWS_RESOURCE_DISCOVERY", summary);
        } catch (AwsAccessDeniedException e) {
            log.warn("AccessDenied during resources discovery for region: {}", region);
            return SubsystemSnapshot.denied("AWS_RESOURCE_DISCOVERY", "AccessDenied", e.getMessage());
        } catch (Exception e) {
            log.error("Error during resources discovery for region: {}", region, e);
            return SubsystemSnapshot.error("AWS_RESOURCE_DISCOVERY", "UNAVAILABLE", e.getMessage());
        }
    }

    private SubsystemSnapshot<TopologyGraph> fetchTopologySubsystem(String region) {
        try {
            TopologyGraph graph = topologyQueryService.getTopology(region);
            if (graph == null || graph.nodes() == null || graph.nodes().isEmpty()) {
                return SubsystemSnapshot.empty("AWS_TOPOLOGY_BUILDER", graph != null ? graph : new TopologyGraph(resolveAccountId(), region, Instant.now(), 0, 0, List.of(), List.of()));
            }
            return SubsystemSnapshot.live("AWS_TOPOLOGY_BUILDER", graph);
        } catch (AwsAccessDeniedException e) {
            log.warn("AccessDenied during topology build for region: {}", region);
            return SubsystemSnapshot.denied("AWS_TOPOLOGY_BUILDER", "AccessDenied", e.getMessage());
        } catch (Exception e) {
            log.error("Error during topology build for region: {}", region, e);
            return SubsystemSnapshot.error("AWS_TOPOLOGY_BUILDER", "UNAVAILABLE", e.getMessage());
        }
    }

    private SubsystemSnapshot<ComplianceEvaluationReport> fetchComplianceSubsystem(String region) {
        try {
            ComplianceEvaluationReport report = complianceService.evaluateLocal(region, List.of());
            return SubsystemSnapshot.live("AWS_COMPLIANCE_ENGINE", report);
        } catch (AwsAccessDeniedException e) {
            log.warn("AccessDenied during compliance evaluation for region: {}", region);
            return SubsystemSnapshot.denied("AWS_COMPLIANCE_ENGINE", "AccessDenied", e.getMessage());
        } catch (Exception e) {
            log.error("Error during compliance evaluation for region: {}", region, e);
            return SubsystemSnapshot.error("AWS_COMPLIANCE_ENGINE", "UNAVAILABLE", e.getMessage());
        }
    }

    private SubsystemSnapshot<CostAggregationResult> fetchCostsSubsystem() {
        try {
            CostAggregationResult costs = costService.getCostAndUsage("UnblendedCost", "MONTHLY", null, null, null, null);
            return SubsystemSnapshot.live("AWS_COST_EXPLORER_ACCOUNT_WIDE", costs);
        } catch (AwsAccessDeniedException e) {
            log.warn("AccessDenied during Cost Explorer query");
            return SubsystemSnapshot.denied("AWS_COST_EXPLORER_ACCOUNT_WIDE", "AccessDenied", e.getMessage());
        } catch (Exception e) {
            log.error("Error during Cost Explorer query", e);
            return SubsystemSnapshot.error("AWS_COST_EXPLORER_ACCOUNT_WIDE", "UNAVAILABLE", e.getMessage());
        }
    }

    public String resolveRegion(String optionalRegion) {
        return (optionalRegion != null && !optionalRegion.isBlank()) ? optionalRegion.trim() : defaultRegion;
    }

    public String resolveAccountId() {
        return awsIdentityService.getCurrentIdentity().accountId();
    }
}
