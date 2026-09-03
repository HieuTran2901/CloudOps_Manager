package com.cloudops.manager.aws.dashboard.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "cloudops.dashboard.cache.type", havingValue = "memory", matchIfMissing = true)
public class InMemoryDashboardRefreshLock implements DashboardRefreshLock {

    private static final Logger log = LoggerFactory.getLogger(InMemoryDashboardRefreshLock.class);
    private final Map<String, Instant> locks = new ConcurrentHashMap<>();

    @Override
    public synchronized boolean tryLock(String accountId, String region, long lockTtlSeconds) {
        String key = buildKey(accountId, region);
        Instant now = Instant.now();
        Instant expiresAt = locks.get(key);

        if (expiresAt != null && expiresAt.isAfter(now)) {
            log.info("DASHBOARD_SNAPSHOT_REFRESH_LOCKED for key: {}", key);
            return false;
        }

        locks.put(key, now.plusSeconds(lockTtlSeconds));
        log.info("Acquired refresh lock for key: {}, ttl: {}s", key, lockTtlSeconds);
        return true;
    }

    @Override
    public synchronized void unlock(String accountId, String region) {
        String key = buildKey(accountId, region);
        locks.remove(key);
        log.info("Released refresh lock for key: {}", key);
    }

    @Override
    public boolean isLocked(String accountId, String region) {
        String key = buildKey(accountId, region);
        Instant expiresAt = locks.get(key);
        return expiresAt != null && expiresAt.isAfter(Instant.now());
    }

    private String buildKey(String accountId, String region) {
        return "lock:" + accountId.trim() + ":" + region.trim();
    }
}
