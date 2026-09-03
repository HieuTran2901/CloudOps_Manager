package com.cloudops.manager.aws.dashboard.service;

import com.cloudops.manager.aws.compliance.model.ComplianceEvaluationReport;
import com.cloudops.manager.aws.compliance.service.ComplianceEvaluationService;
import com.cloudops.manager.aws.cost.model.CostAggregationResult;
import com.cloudops.manager.aws.cost.service.CostObservabilityService;
import com.cloudops.manager.aws.dashboard.cache.InMemoryDashboardRefreshLock;
import com.cloudops.manager.aws.dashboard.cache.InMemoryDashboardSnapshotCache;
import com.cloudops.manager.aws.dashboard.model.DashboardSnapshot;
import com.cloudops.manager.aws.dashboard.model.DashboardSnapshotStatus;
import com.cloudops.manager.aws.discovery.model.InventorySummary;

import com.cloudops.manager.aws.discovery.model.CloudResourceType;
import com.cloudops.manager.aws.discovery.service.AwsResourceDiscoveryService;
import com.cloudops.manager.aws.sts.model.CallerIdentity;
import com.cloudops.manager.aws.sts.service.AwsIdentityService;
import com.cloudops.manager.aws.topology.model.TopologyGraph;
import com.cloudops.manager.aws.topology.service.TopologyQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DashboardSnapshotServiceTest {

    private AwsResourceDiscoveryService discoveryService;
    private TopologyQueryService topologyQueryService;
    private ComplianceEvaluationService complianceService;
    private CostObservabilityService costService;
    private AwsIdentityService awsIdentityService;
    private InMemoryDashboardSnapshotCache snapshotCache;
    private InMemoryDashboardRefreshLock refreshLock;
    private DashboardSnapshotService snapshotService;

    @BeforeEach
    void setUp() {
        discoveryService = mock(AwsResourceDiscoveryService.class);
        topologyQueryService = mock(TopologyQueryService.class);
        complianceService = mock(ComplianceEvaluationService.class);
        costService = mock(CostObservabilityService.class);
        awsIdentityService = mock(AwsIdentityService.class);
        snapshotCache = new InMemoryDashboardSnapshotCache();
        refreshLock = new InMemoryDashboardRefreshLock();

        when(awsIdentityService.getCurrentIdentity()).thenReturn(
                new CallerIdentity("351405419700", "arn:aws:iam::351405419700:user/agent", "agent")
        );

        when(discoveryService.discoverAll(anyString())).thenReturn(
                new InventorySummary("351405419700", "ap-southeast-2", 0, Map.of(CloudResourceType.EC2_INSTANCE, 0), List.of(), Instant.now())
        );
        when(topologyQueryService.getTopology(anyString())).thenReturn(
                new TopologyGraph("351405419700", "ap-southeast-2", Instant.now(), 0, 0, List.of(), List.of())
        );
        when(complianceService.evaluateLocal(anyString(), any())).thenReturn(
                new ComplianceEvaluationReport("351405419700", "ap-southeast-2", Instant.now(), 5, 5, 0, 0, 0, List.of())
        );
        when(costService.getCostAndUsage(anyString(), anyString(), any(), any(), any(), any())).thenReturn(
                new CostAggregationResult("351405419700", "ACCOUNT_WIDE", "UnblendedCost", "MONTHLY", null, java.math.BigDecimal.ZERO, "USD", List.of(), Instant.now())
        );

        snapshotService = new DashboardSnapshotService(
                discoveryService, topologyQueryService, complianceService, costService,
                awsIdentityService, snapshotCache, refreshLock
        );
    }

    @Test
    void shouldPerformInitialIngestionOnCacheMiss() {
        DashboardSnapshot snapshot = snapshotService.getSnapshot("ap-southeast-2");

        assertNotNull(snapshot);
        assertEquals("351405419700", snapshot.accountId());
        assertEquals("ap-southeast-2", snapshot.region());
        assertEquals(DashboardSnapshotStatus.LIVE, snapshot.snapshotStatus());

        verify(discoveryService, times(1)).discoverAll("ap-southeast-2");
        verify(topologyQueryService, times(1)).getTopology("ap-southeast-2");
    }

    @Test
    void shouldReturnFreshCachedSnapshotWithoutRevalidation() {
        DashboardSnapshot first = snapshotService.getSnapshot("ap-southeast-2");
        assertEquals(DashboardSnapshotStatus.LIVE, first.snapshotStatus());

        // Second call within fresh TTL (60s)
        DashboardSnapshot second = snapshotService.getSnapshot("ap-southeast-2");
        assertEquals(DashboardSnapshotStatus.LIVE, second.snapshotStatus());

        // Providers called only once during initial ingestion
        verify(discoveryService, times(1)).discoverAll("ap-southeast-2");
    }

    @Test
    void shouldEnforceRegionIsolationInCache() {
        snapshotService.getSnapshot("ap-southeast-2");
        snapshotService.getSnapshot("us-east-1");

        verify(discoveryService, times(1)).discoverAll("ap-southeast-2");
        verify(discoveryService, times(1)).discoverAll("us-east-1");
    }
}
