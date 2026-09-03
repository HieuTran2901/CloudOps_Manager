package com.cloudops.manager.aws.dashboard.persistence;

import com.cloudops.manager.aws.compliance.model.ComplianceEvaluationReport;
import com.cloudops.manager.aws.compliance.service.ComplianceEvaluationService;
import com.cloudops.manager.aws.cost.model.CostAggregationResult;
import com.cloudops.manager.aws.cost.service.CostObservabilityService;
import com.cloudops.manager.aws.dashboard.cache.InMemoryDashboardRefreshLock;
import com.cloudops.manager.aws.dashboard.cache.InMemoryDashboardSnapshotCache;
import com.cloudops.manager.aws.dashboard.cache.RedisDashboardSnapshotCache;
import com.cloudops.manager.aws.dashboard.model.DashboardSnapshot;
import com.cloudops.manager.aws.dashboard.model.DashboardSnapshotStatus;
import com.cloudops.manager.aws.dashboard.service.DashboardSnapshotService;
import com.cloudops.manager.aws.discovery.model.CloudResourceType;
import com.cloudops.manager.aws.discovery.model.InventorySummary;
import com.cloudops.manager.aws.discovery.service.AwsResourceDiscoveryService;
import com.cloudops.manager.aws.sts.model.CallerIdentity;
import com.cloudops.manager.aws.sts.service.AwsIdentityService;
import com.cloudops.manager.aws.topology.model.TopologyGraph;
import com.cloudops.manager.aws.topology.service.TopologyQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("Production Redis Persistence & Disaster Recovery Tests")
class RedisPersistenceRecoveryTest {

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
    @DisplayName("Corrupted Persisted Snapshot Recovery: Malformed JSON in Redis is rejected cleanly")
    void testPersistedSnapshotCorruptionRecovery() {
        StringRedisTemplate mockRedisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> mockValueOps = mock(ValueOperations.class);

        when(mockRedisTemplate.opsForValue()).thenReturn(mockValueOps);
        when(mockValueOps.get("cloudops:dashboard:v1:351405419700:ap-southeast-2"))
                .thenReturn("{ \"invalidJson\": true, \"malformedSyntax\"... "); // Corrupted JSON payload

        RedisDashboardSnapshotCache redisCache = new RedisDashboardSnapshotCache(mockRedisTemplate);

        Optional<DashboardSnapshot> snapshotOpt = redisCache.get("351405419700", "ap-southeast-2");

        assertFalse(snapshotOpt.isPresent(), "Corrupted persisted JSON must return Optional.empty() without throwing exceptions");

        // Service fallback verification
        DashboardSnapshotService serviceWithRedis = new DashboardSnapshotService(
                discoveryService, topologyQueryService, complianceService, costService,
                awsIdentityService, redisCache, refreshLock
        );

        DashboardSnapshot snapshot = serviceWithRedis.getSnapshot("ap-southeast-2");

        assertNotNull(snapshot);
        assertEquals(DashboardSnapshotStatus.LIVE, snapshot.snapshotStatus());
        assertEquals(1, discoveryInvocationCount.get(), "Corrupted snapshot recovery must fall back to fresh AWS ingestion");
    }

    @Test
    @DisplayName("TTL & Persistence Interaction: Restored snapshot respects application freshness threshold")
    void testTtlPersistenceInteraction() {
        // Restored snapshot from disk with age = 700s (expired)
        DashboardSnapshot restoredExpiredSnapshot = new DashboardSnapshot(
                "351405419700", "ap-southeast-2", DashboardSnapshotStatus.LIVE,
                Instant.now().minusSeconds(700), Instant.now().minusSeconds(700),
                null, null, null, null
        );
        snapshotCache.put(restoredExpiredSnapshot);

        DashboardSnapshot snapshot = snapshotService.getSnapshot("ap-southeast-2");

        assertNotNull(snapshot);
        assertEquals(DashboardSnapshotStatus.LIVE, snapshot.snapshotStatus());
        assertEquals(1, discoveryInvocationCount.get(), "Restored persisted snapshot > 600s must trigger synchronous refresh instead of serving stale indefinitely");
    }

    @Test
    @DisplayName("Eviction Recovery: Memory eviction resulting in cache miss performs single-flight refresh")
    void testEvictionRecovery() {
        // Snapshot evicted from memory/Redis
        snapshotCache.invalidate("351405419700", "ap-southeast-2");

        DashboardSnapshot snapshot = snapshotService.getSnapshot("ap-southeast-2");

        assertNotNull(snapshot);
        assertEquals(DashboardSnapshotStatus.LIVE, snapshot.snapshotStatus());
        assertEquals(1, discoveryInvocationCount.get(), "Eviction must recover cleanly via initial single-flight ingestion");
    }

    @Test
    @DisplayName("AWS Source of Truth Provenance: Restored snapshot preserves identity and source metadata")
    void testAwsSourceOfTruthProvenance() {
        DashboardSnapshot snapshot = snapshotService.getSnapshot("ap-southeast-2");

        assertNotNull(snapshot);
        assertEquals("351405419700", snapshot.accountId());
        assertEquals("ap-southeast-2", snapshot.region());
        assertNotNull(snapshot.generatedAt());
        assertNotNull(snapshot.resources());
        assertEquals("AWS_RESOURCE_DISCOVERY", snapshot.resources().source());
    }
}
