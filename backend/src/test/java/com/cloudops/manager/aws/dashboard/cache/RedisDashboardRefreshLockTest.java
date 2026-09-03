package com.cloudops.manager.aws.dashboard.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RedisDashboardRefreshLockTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private RedisDashboardRefreshLock refreshLock;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        refreshLock = new RedisDashboardRefreshLock(redisTemplate);
    }

    @Test
    void shouldAcquireLockWhenKeyAbsent() {
        when(valueOperations.setIfAbsent(eq("cloudops:dashboard:refresh:351405419700:ap-southeast-2"), anyString(), any(Duration.class)))
                .thenReturn(true);

        boolean locked = refreshLock.tryLock("351405419700", "ap-southeast-2", 120);

        assertTrue(locked);
    }

    @Test
    void shouldRejectLockWhenAlreadyHeld() {
        when(valueOperations.setIfAbsent(eq("cloudops:dashboard:refresh:351405419700:ap-southeast-2"), anyString(), any(Duration.class)))
                .thenReturn(false);

        boolean locked = refreshLock.tryLock("351405419700", "ap-southeast-2", 120);

        assertFalse(locked);
    }

    @Test
    void shouldHandleRedisExceptionGracefully() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenThrow(new RuntimeException("Redis connection refused"));

        // Fallback policy: allow execution if Redis is unavailable to prevent deadlock
        boolean locked = refreshLock.tryLock("351405419700", "ap-southeast-2", 120);

        assertTrue(locked);
    }
}
