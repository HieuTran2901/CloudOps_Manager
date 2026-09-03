package com.cloudops.manager.aws.dashboard.cache;

import com.cloudops.manager.aws.dashboard.model.DashboardSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "cloudops.dashboard.cache.type", havingValue = "redis", matchIfMissing = false)
public class RedisDashboardSnapshotCache implements DashboardSnapshotCache {

    private static final Logger log = LoggerFactory.getLogger(RedisDashboardSnapshotCache.class);
    private static final String KEY_PREFIX = "cloudops:dashboard:v1:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${cloudops.dashboard.snapshot.stale-ttl-seconds:600}")
    private long staleTtlSeconds;

    public RedisDashboardSnapshotCache(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public Optional<DashboardSnapshot> get(String accountId, String region) {
        String key = buildKey(accountId, region);
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json != null && !json.isBlank()) {
                DashboardSnapshot snapshot = objectMapper.readValue(json, DashboardSnapshot.class);
                log.info("DASHBOARD_SNAPSHOT_CACHE_HIT (Redis) for key: {}", key);
                return Optional.of(snapshot);
            }
            log.info("DASHBOARD_SNAPSHOT_CACHE_MISS (Redis) for key: {}", key);
            return Optional.empty();
        } catch (Exception e) {
            log.warn("REDIS_UNAVAILABLE on get for key: {}. Falling back to cache miss: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void put(DashboardSnapshot snapshot) {
        if (snapshot == null || snapshot.accountId() == null || snapshot.region() == null) {
            return;
        }
        String key = buildKey(snapshot.accountId(), snapshot.region());
        try {
            String json = objectMapper.writeValueAsString(snapshot);
            redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(staleTtlSeconds));
            log.info("DASHBOARD_SNAPSHOT_ATOMIC_REPLACE (Redis) for key: {}, ttl: {}s, status: {}", key, staleTtlSeconds, snapshot.snapshotStatus());
        } catch (Exception e) {
            log.error("REDIS_UNAVAILABLE or serialization error on put for key: {}", key, e);
        }
    }

    @Override
    public void invalidate(String accountId, String region) {
        String key = buildKey(accountId, region);
        try {
            redisTemplate.delete(key);
            log.info("Invalidated Redis snapshot key: {}", key);
        } catch (Exception e) {
            log.warn("REDIS_UNAVAILABLE on invalidate for key: {}", key, e);
        }
    }

    @Override
    public boolean exists(String accountId, String region) {
        String key = buildKey(accountId, region);
        try {
            Boolean hasKey = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(hasKey);
        } catch (Exception e) {
            log.warn("REDIS_UNAVAILABLE on exists for key: {}", key, e);
            return false;
        }
    }

    private String buildKey(String accountId, String region) {
        return KEY_PREFIX + accountId.trim() + ":" + region.trim();
    }
}
