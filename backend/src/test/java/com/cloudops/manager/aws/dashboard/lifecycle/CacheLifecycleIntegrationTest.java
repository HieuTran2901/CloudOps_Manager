package com.cloudops.manager.aws.dashboard.lifecycle;

import com.cloudops.manager.aws.compliance.model.ComplianceEvaluationReport;
import com.cloudops.manager.aws.compliance.service.ComplianceEvaluationService;
import com.cloudops.manager.aws.cost.model.CostAggregationResult;
import com.cloudops.manager.aws.cost.service.CostObservabilityService;
import com.cloudops.manager.aws.dashboard.cache.InMemoryDashboardRefreshLock;
import com.cloudops.manager.aws.dashboard.cache.InMemoryDashboardSnapshotCache;
import com.cloudops.manager.aws.dashboard.model.DashboardSnapshot;
import com.cloudops.manager.aws.dashboard.model.DashboardSnapshotStatus;
import com.cloudops.manager.aws.dashboard.model.SubsystemSnapshot;
import com.cloudops.manager.aws.dashboard.service.DashboardSnapshotService;
import com.cloudops.manager.aws.discovery.model.CloudResourceType;
import com.cloudops.manager.aws.discovery.model.InventorySummary;
import com.cloudops.manager.aws.discovery.service.AwsResourceDiscoveryService;
import com.cloudops.manager.aws.sts.model.CallerIdentity;
import com.cloudops.manager.aws.sts.service.AwsIdentityService;
import com.cloudops.manager.aws.topology.model.TopologyGraph;
import com.cloudops.manager.aws.topology.service.TopologyQueryService;
import com.cloudops.manager.aws.dashboard.cache.DashboardSnapshotCache;
import com.cloudops.manager.common.exception.AwsServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("Production Cache Lifecycle & Invalidation Integration Tests")
class CacheLifecycleIntegrationTest {

    private AwsResourceDiscoveryService discoveryService;
    private TopologyQueryService topologyQueryService;
    private ComplianceEvaluationService complianceService;
    private CostObservabilityService costService;
    private AwsIdentityService awsIdentityService;
    private InMemoryDashboardSnapshotCache snapshotCache;
    private InMemoryDashboardRefreshLock refreshLock;
    private DashboardSnapshotService snapshotService;

    private AtomicInteger discoveryInvocationCount;

    @BeforeEach
    void setUp() {
        discoveryService = mock(AwsResourceDiscoveryService.class);
        topologyQueryService = mock(TopologyQueryService.class);
        complianceService = mock(ComplianceEvaluationService.class);
        costService = mock(CostObservabilityService.class);
        awsIdentityService = mock(AwsIdentityService.class);
        snapshotCache = new InMemoryDashboardSnapshotCache();
        refreshLock = new InMemoryDashboardRefreshLock();

        discoveryInvocationCount = new AtomicInteger(0);

        when(awsIdentityService.getCurrentIdentity()).thenReturn(
                new CallerIdentity("351405419700", "arn:aws:iam::351405419700:user/agent", "agent")
        );

        when(discoveryService.discoverAll(anyString())).thenAnswer(invocation -> {
            discoveryInvocationCount.incrementAndGet();
            return new InventorySummary("351405419700", invocation.getArgument(0), 11, Map.of(CloudResourceType.EC2_INSTANCE, 1), List.of(), Instant.now());
        });

        when(topologyQueryService.getTopology(anyString())).thenReturn(
                new TopologyGraph("351405419700", "ap-southeast-2", Instant.now(), 14, 5, List.of(), List.of())
        );
        when(complianceService.evaluateLocal(anyString(), anyList())).thenReturn(
                new ComplianceEvaluationReport("351405419700", "ap-southeast-2", Instant.now(), 6, 4, 2, 0, 0, List.of())
        );
        when(costService.getCostAndUsage(anyString(), anyString(), any(), any(), any(), any())).thenReturn(
                new CostAggregationResult("351405419700", "ACCOUNT_WIDE", "UnblendedCost", "MONTHLY", null, BigDecimal.ZERO, "USD", List.of(), Instant.now())
        );

        snapshotService = new DashboardSnapshotService(
                discoveryService, topologyQueryService, complianceService, costService,
                awsIdentityService, snapshotCache, refreshLock
        );
    }

    @Test
    @DisplayName("FRESH Lifecycle State: Snapshot <= 60s serves LIVE immediately with 0 AWS calls")
    void testFreshState() {
        DashboardSnapshot freshSnapshot = new DashboardSnapshot(
                "351405419700", "ap-southeast-2", DashboardSnapshotStatus.LIVE,
                Instant.now().minusSeconds(10), Instant.now().minusSeconds(10),
                null, null, null, null
        );
        snapshotCache.put(freshSnapshot);

        DashboardSnapshot snapshot = snapshotService.getSnapshot("ap-southeast-2");

        assertNotNull(snapshot);
        assertEquals(DashboardSnapshotStatus.LIVE, snapshot.snapshotStatus());
        assertEquals(0, discoveryInvocationCount.get(), "Fresh snapshot must serve from cache without AWS calls");
    }

    @Test
    @DisplayName("STALE Lifecycle State: 60s < Age <= 600s returns STALE immediately and triggers background refresh")
    void testStaleState() throws Exception {
        DashboardSnapshot staleSnapshot = new DashboardSnapshot(
                "351405419700", "ap-southeast-2", DashboardSnapshotStatus.STALE,
                Instant.now().minusSeconds(120), Instant.now().minusSeconds(120),
                null, null, null, null
        );
        snapshotCache.put(staleSnapshot);

        DashboardSnapshot snapshot = snapshotService.getSnapshot("ap-southeast-2");

        assertNotNull(snapshot);
        assertEquals(DashboardSnapshotStatus.STALE, snapshot.snapshotStatus());

        // Wait brief moment for async background refresh to finish
        Thread.sleep(100);

        assertEquals(1, discoveryInvocationCount.get(), "Stale snapshot must trigger 1 background refresh");

        Optional<DashboardSnapshot> updatedCache = snapshotCache.get("351405419700", "ap-southeast-2");
        assertTrue(updatedCache.isPresent());
        assertEquals(DashboardSnapshotStatus.LIVE, updatedCache.get().snapshotStatus());
    }

    @Test
    @DisplayName("EXPIRED Lifecycle State: Age > 600s performs synchronous refresh")
    void testExpiredState() {
        DashboardSnapshot expiredSnapshot = new DashboardSnapshot(
                "351405419700", "ap-southeast-2", DashboardSnapshotStatus.STALE,
                Instant.now().minusSeconds(700), Instant.now().minusSeconds(700),
                null, null, null, null
        );
        snapshotCache.put(expiredSnapshot);

        DashboardSnapshot snapshot = snapshotService.getSnapshot("ap-southeast-2");

        assertNotNull(snapshot);
        assertEquals(DashboardSnapshotStatus.LIVE, snapshot.snapshotStatus());
        assertEquals(1, discoveryInvocationCount.get(), "Expired snapshot must perform synchronous refresh");
    }

    @Test
    @DisplayName("MISSING Lifecycle State: Cache miss performs initial ingestion")
    void testMissingState() {
        DashboardSnapshot snapshot = snapshotService.getSnapshot("ap-southeast-2");

        assertNotNull(snapshot);
        assertEquals(DashboardSnapshotStatus.LIVE, snapshot.snapshotStatus());
        assertEquals(1, discoveryInvocationCount.get(), "Missing snapshot must perform initial ingestion");
    }

    @Test
    @DisplayName("FAILED_REFRESH State: Refresh exception preserves previous valid snapshot with REFRESH_FAILED status")
    void testFailedRefreshPreservesLastValidSnapshot() {
        DashboardSnapshotCache spyCache = spy(snapshotCache);
        DashboardSnapshotService serviceWithSpyCache = new DashboardSnapshotService(
                discoveryService, topologyQueryService, complianceService, costService,
                awsIdentityService, spyCache, refreshLock
        );

        DashboardSnapshot expiredSnapshot = new DashboardSnapshot(
                "351405419700", "ap-southeast-2", DashboardSnapshotStatus.STALE,
                Instant.now().minusSeconds(700), Instant.now().minusSeconds(700),
                SubsystemSnapshot.live("AWS_RESOURCE_DISCOVERY", new InventorySummary("351405419700", "ap-southeast-2", 11, Map.of(CloudResourceType.EC2_INSTANCE, 1), List.of(), Instant.now())),
                null, null, null
        );
        spyCache.put(expiredSnapshot);

        doThrow(new RuntimeException("Cache write error during refresh")).when(spyCache).put(argThat(s -> s instanceof DashboardSnapshot snapshot && snapshot.snapshotStatus() == DashboardSnapshotStatus.LIVE));

        DashboardSnapshot snapshot = serviceWithSpyCache.getSnapshot("ap-southeast-2");

        assertNotNull(snapshot);
        assertEquals(DashboardSnapshotStatus.REFRESH_FAILED, snapshot.snapshotStatus());
        assertNotNull(snapshot.resources());
        assertEquals(11, snapshot.resources().data().totalCount(), "Failed refresh must preserve previous valid snapshot data");
    }

    @Test
    @DisplayName("Subsystem Failure: Subsystem exception marks subsystem ERROR without destroying snapshot structure")
    void testSubsystemFailurePreservesStructure() {
        when(discoveryService.discoverAll("ap-southeast-2")).thenThrow(new AwsServiceUnavailableException("503 Service Unavailable"));

        DashboardSnapshot snapshot = snapshotService.getSnapshot("ap-southeast-2");

        assertNotNull(snapshot);
        assertEquals(DashboardSnapshotStatus.LIVE, snapshot.snapshotStatus());
        assertNotNull(snapshot.resources());
        assertEquals(com.cloudops.manager.aws.dashboard.model.SubsystemStatus.ERROR, snapshot.resources().status());
    }

    @Test
    @DisplayName("Backend Restart Cache Reuse: Restarted JVM reuses existing cache without ingestion")
    void testBackendRestartCacheReuse() {
        DashboardSnapshot existingSnapshot = new DashboardSnapshot(
                "351405419700", "ap-southeast-2", DashboardSnapshotStatus.LIVE,
                Instant.now().minusSeconds(15), Instant.now().minusSeconds(15),
                null, null, null, null
        );
        snapshotCache.put(existingSnapshot);

        // Instantiate new service instance simulating JVM backend process restart
        DashboardSnapshotService newBackendInstance = new DashboardSnapshotService(
                discoveryService, topologyQueryService, complianceService, costService,
                awsIdentityService, snapshotCache, refreshLock
        );

        DashboardSnapshot snapshot = newBackendInstance.getSnapshot("ap-southeast-2");

        assertNotNull(snapshot);
        assertEquals(DashboardSnapshotStatus.LIVE, snapshot.snapshotStatus());
        assertEquals(0, discoveryInvocationCount.get(), "Backend restart must reuse existing snapshot from shared cache");
    }

    @Test
    @DisplayName("Invalidation Scope Isolation: Invalidation removes only target region")
    void testInvalidationScopeIsolation() {
        snapshotCache.put(new DashboardSnapshot("351405419700", "ap-southeast-2", DashboardSnapshotStatus.LIVE, Instant.now(), Instant.now(), null, null, null, null));
        snapshotCache.put(new DashboardSnapshot("351405419700", "us-east-1", DashboardSnapshotStatus.LIVE, Instant.now(), Instant.now(), null, null, null, null));

        snapshotCache.invalidate("351405419700", "ap-southeast-2");

        assertFalse(snapshotCache.exists("351405419700", "ap-southeast-2"));
        assertTrue(snapshotCache.exists("351405419700", "us-east-1"), "Invalidation of ap-southeast-2 must not affect us-east-1");
    }
}
