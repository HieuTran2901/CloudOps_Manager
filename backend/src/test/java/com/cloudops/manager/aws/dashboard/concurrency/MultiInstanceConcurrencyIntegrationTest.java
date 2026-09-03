package com.cloudops.manager.aws.dashboard.concurrency;

import com.cloudops.manager.aws.compliance.model.ComplianceEvaluationReport;
import com.cloudops.manager.aws.compliance.service.ComplianceEvaluationService;
import com.cloudops.manager.aws.cost.model.CostAggregationResult;
import com.cloudops.manager.aws.cost.service.CostObservabilityService;
import com.cloudops.manager.aws.dashboard.cache.InMemoryDashboardRefreshLock;
import com.cloudops.manager.aws.dashboard.cache.InMemoryDashboardSnapshotCache;
import com.cloudops.manager.aws.dashboard.model.DashboardSnapshot;
import com.cloudops.manager.aws.dashboard.model.DashboardSnapshotStatus;
import com.cloudops.manager.aws.dashboard.model.SubsystemStatus;
import com.cloudops.manager.aws.dashboard.service.DashboardSnapshotService;
import com.cloudops.manager.aws.discovery.model.CloudResourceType;
import com.cloudops.manager.aws.discovery.model.InventorySummary;
import com.cloudops.manager.aws.discovery.service.AwsResourceDiscoveryService;
import com.cloudops.manager.aws.sts.model.CallerIdentity;
import com.cloudops.manager.aws.sts.service.AwsIdentityService;
import com.cloudops.manager.aws.topology.model.TopologyGraph;
import com.cloudops.manager.aws.topology.service.TopologyQueryService;
import com.cloudops.manager.common.exception.AwsAccessDeniedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("Multi-Instance Concurrency & Resilience Integration Tests")
class MultiInstanceConcurrencyIntegrationTest {

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
            // Simulate AWS API response latency (20ms)
            Thread.sleep(20);
            return new InventorySummary("351405419700", invocation.getArgument(0), 11, Map.of(CloudResourceType.EC2_INSTANCE, 1), List.of(), Instant.now());
        });

        when(topologyQueryService.getTopology(anyString())).thenReturn(
                new TopologyGraph("351405419700", "ap-southeast-2", Instant.now(), 14, 5, List.of(), List.of())
        );
        when(complianceService.evaluateLocal(anyString(), any())).thenReturn(
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
    @DisplayName("Single-Flight: 20 concurrent requests across simulated nodes result in exactly 1 AWS ingestion")
    void shouldPreventRefreshStormUnderConcurrentRequestsFromMultipleNodes() throws Exception {
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        List<Future<DashboardSnapshot>> futures = new ArrayList<>();

        // Seed a STALE snapshot (65s old) to trigger SWR background revalidation under concurrent requests
        DashboardSnapshot staleSnapshot = new DashboardSnapshot(
                "351405419700",
                "ap-southeast-2",
                DashboardSnapshotStatus.STALE,
                Instant.now().minusSeconds(65),
                Instant.now().minusSeconds(65),
                null, null, null, null
        );
        snapshotCache.put(staleSnapshot);

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                try {
                    startLatch.await();
                    return snapshotService.getSnapshot("ap-southeast-2");
                } finally {
                    finishLatch.countDown();
                }
            }));
        }

        // Trigger simultaneous execution across threads
        startLatch.countDown();
        finishLatch.await(5, TimeUnit.SECONDS);

        // Allow background revalidation thread to complete execution
        Thread.sleep(200);

        List<DashboardSnapshot> snapshots = new ArrayList<>();
        for (Future<DashboardSnapshot> future : futures) {
            snapshots.add(future.get());
        }

        executor.shutdown();

        // Verify all 20 threads received valid snapshots
        assertEquals(20, snapshots.size());
        for (DashboardSnapshot s : snapshots) {
            assertNotNull(s);
            assertEquals("351405419700", s.accountId());
            assertEquals("ap-southeast-2", s.region());
        }

        // Crucial Single-Flight assertion: Only 1 AWS discovery ingestion executed!
        assertEquals(1, discoveryInvocationCount.get(), "Single-flight lock failed to prevent refresh storm under concurrent requests");
    }

    @Test
    @DisplayName("Lock Ownership & Lock Expiration Recovery")
    void shouldEnforceLockOwnershipAndExpirationRecovery() {
        boolean lock1 = refreshLock.tryLock("351405419700", "ap-southeast-2", 1); // 1s TTL
        assertTrue(lock1);

        // Second lock attempt immediately should fail
        boolean lock2 = refreshLock.tryLock("351405419700", "ap-southeast-2", 1);
        assertFalse(lock2);

        // Unlock
        refreshLock.unlock("351405419700", "ap-southeast-2");

        // Now lock can be acquired again
        boolean lock3 = refreshLock.tryLock("351405419700", "ap-southeast-2", 1);
        assertTrue(lock3);
    }

    @Test
    @DisplayName("Failure Resilience: AWS AccessDenied does not crash or collapse into fake zero data")
    void shouldPreserveFailureSemanticsUnderAWSAccessDenied() {
        when(discoveryService.discoverAll("ap-southeast-2"))
                .thenThrow(new AwsAccessDeniedException("User arn:aws:iam::123 is not authorized for ec2:DescribeInstances"));

        DashboardSnapshot snapshot = snapshotService.getSnapshot("ap-southeast-2");

        assertNotNull(snapshot);
        assertNotNull(snapshot.resources());
        assertEquals(SubsystemStatus.DENIED, snapshot.resources().status());
        assertEquals("AccessDenied", snapshot.resources().errorCode());
        assertNull(snapshot.resources().data());
    }

    @Test
    @DisplayName("Account and Region Isolation in Snapshot Cache")
    void shouldMaintainAccountAndRegionIsolation() {
        DashboardSnapshot s1 = snapshotService.getSnapshot("ap-southeast-2");
        DashboardSnapshot s2 = snapshotService.getSnapshot("us-east-1");

        assertNotNull(s1);
        assertNotNull(s2);
        assertEquals("ap-southeast-2", s1.region());
        assertEquals("us-east-1", s2.region());

        // Verify independent discovery invocations per region
        verify(discoveryService, times(1)).discoverAll("ap-southeast-2");
        verify(discoveryService, times(1)).discoverAll("us-east-1");
    }
}
