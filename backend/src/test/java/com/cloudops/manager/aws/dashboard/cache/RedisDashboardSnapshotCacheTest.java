package com.cloudops.manager.aws.dashboard.cache;

import com.cloudops.manager.aws.dashboard.model.DashboardSnapshot;
import com.cloudops.manager.aws.dashboard.model.DashboardSnapshotStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RedisDashboardSnapshotCacheTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private RedisDashboardSnapshotCache cache;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        cache = new RedisDashboardSnapshotCache(redisTemplate);
    }

    @Test
    void shouldReturnEmptyOnCacheMiss() {
        when(valueOperations.get("cloudops:dashboard:v1:351405419700:ap-southeast-2")).thenReturn(null);

        Optional<DashboardSnapshot> snapshot = cache.get("351405419700", "ap-southeast-2");

        assertTrue(snapshot.isEmpty());
        verify(valueOperations, times(1)).get("cloudops:dashboard:v1:351405419700:ap-southeast-2");
    }

    @Test
    void shouldHandleRedisFailureGracefullyWithoutCrashing() {
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis connection refused"));

        Optional<DashboardSnapshot> snapshot = cache.get("351405419700", "ap-southeast-2");

        assertTrue(snapshot.isEmpty());
    }

    @Test
    void shouldIsolateKeysByAccountAndRegion() {
        DashboardSnapshot snapshotAp = new DashboardSnapshot(
                "351405419700", "ap-southeast-2", DashboardSnapshotStatus.LIVE, Instant.now(), Instant.now(), null, null, null, null
        );

        cache.put(snapshotAp);

        verify(valueOperations, times(1)).set(
                eq("cloudops:dashboard:v1:351405419700:ap-southeast-2"),
                anyString(),
                any()
        );
    }
}
