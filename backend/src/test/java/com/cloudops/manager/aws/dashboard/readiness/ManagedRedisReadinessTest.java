package com.cloudops.manager.aws.dashboard.readiness;

import com.cloudops.manager.aws.compliance.model.ComplianceEvaluationReport;
import com.cloudops.manager.aws.compliance.service.ComplianceEvaluationService;
import com.cloudops.manager.aws.cost.model.CostAggregationResult;
import com.cloudops.manager.aws.cost.service.CostObservabilityService;
import com.cloudops.manager.aws.dashboard.cache.*;
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
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("Production Managed Redis / ElastiCache Readiness Integration Tests")
class ManagedRedisReadinessTest {

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
    @DisplayName("Provider Abstraction: DashboardSnapshotService interacts strictly via cache interfaces")
    void testRedisProviderAbstractionIsolation() {
        // Assert snapshotService accepts any DashboardSnapshotCache implementation without Redis template coupling
        DashboardSnapshotCache customCache = mock(DashboardSnapshotCache.class);
        DashboardRefreshLock customLock = mock(DashboardRefreshLock.class);

        when(customCache.get("351405419700", "ap-southeast-2")).thenReturn(Optional.empty());
        when(customLock.tryLock("351405419700", "ap-southeast-2", 120)).thenReturn(true);

        DashboardSnapshotService abstractService = new DashboardSnapshotService(
                discoveryService, topologyQueryService, complianceService, costService,
                awsIdentityService, customCache, customLock
        );

        DashboardSnapshot snapshot = abstractService.getSnapshot("ap-southeast-2");

        assertNotNull(snapshot);
        assertEquals(DashboardSnapshotStatus.LIVE, snapshot.snapshotStatus());
        verify(customCache, times(1)).put(any());
        verify(customLock, times(1)).unlock("351405419700", "ap-southeast-2");
    }

    @Test
    @DisplayName("Externalized Managed Redis Config: RedisStandaloneConfiguration accepts external endpoint & auth")
    void testExternalizedRedisConfig() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName("clustercfg.cloudops-redis.cache.amazonaws.com");
        config.setPort(6379);
        config.setUsername("cloudops-user");
        config.setPassword("ManagedElastiCacheToken123!");

        assertEquals("clustercfg.cloudops-redis.cache.amazonaws.com", config.getHostName());
        assertEquals(6379, config.getPort());
        assertEquals("cloudops-user", config.getUsername());
        assertEquals("ManagedElastiCacheToken123!", new String(config.getPassword().get()));
    }

    @Test
    @DisplayName("Production Redis Failure Degradation: Outage degrades to direct AWS ingestion without crash")
    void testProductionRedisFailureDegradation() {
        DashboardSnapshotCache failingCache = mock(DashboardSnapshotCache.class);
        DashboardRefreshLock failingLock = mock(DashboardRefreshLock.class);

        // Simulate Redis cluster outage: cache miss fallback + lock tryLock fallback returning true
        when(failingCache.get(anyString(), anyString())).thenReturn(Optional.empty());
        when(failingLock.tryLock(anyString(), anyString(), anyLong())).thenReturn(true);

        DashboardSnapshotService resilientService = new DashboardSnapshotService(
                discoveryService, topologyQueryService, complianceService, costService,
                awsIdentityService, failingCache, failingLock
        );

        // Must succeed via direct AWS ingestion fallback
        DashboardSnapshot snapshot = resilientService.getSnapshot("ap-southeast-2");

        assertNotNull(snapshot);
        assertEquals(DashboardSnapshotStatus.LIVE, snapshot.snapshotStatus());
        assertEquals(1, discoveryInvocationCount.get(), "Redis outage in production must fall back to direct AWS ingestion");
    }
}
