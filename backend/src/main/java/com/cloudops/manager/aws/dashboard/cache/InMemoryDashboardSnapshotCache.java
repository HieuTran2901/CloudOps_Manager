package com.cloudops.manager.aws.dashboard.cache;

import com.cloudops.manager.aws.dashboard.model.DashboardSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "cloudops.dashboard.cache.type", havingValue = "memory", matchIfMissing = true)
public class InMemoryDashboardSnapshotCache implements DashboardSnapshotCache {

    private static final Logger log = LoggerFactory.getLogger(InMemoryDashboardSnapshotCache.class);
    private final Map<String, DashboardSnapshot> store = new ConcurrentHashMap<>();

    @Override
    public Optional<DashboardSnapshot> get(String accountId, String region) {
        String key = buildKey(accountId, region);
        DashboardSnapshot snapshot = store.get(key);
        if (snapshot != null) {
            log.debug("DASHBOARD_SNAPSHOT_CACHE_HIT for key: {}", key);
            return Optional.of(snapshot);
        }
        log.debug("DASHBOARD_SNAPSHOT_CACHE_MISS for key: {}", key);
        return Optional.empty();
    }

    @Override
    public void put(DashboardSnapshot snapshot) {
        if (snapshot == null || snapshot.accountId() == null || snapshot.region() == null) {
            return;
        }
        String key = buildKey(snapshot.accountId(), snapshot.region());
        store.put(key, snapshot);
        log.info("DASHBOARD_SNAPSHOT_ATOMIC_REPLACE for key: {}, status: {}", key, snapshot.snapshotStatus());
    }

    @Override
    public void invalidate(String accountId, String region) {
        String key = buildKey(accountId, region);
        store.remove(key);
        log.info("Invalidated snapshot cache key: {}", key);
    }

    @Override
    public boolean exists(String accountId, String region) {
        return store.containsKey(buildKey(accountId, region));
    }

    private String buildKey(String accountId, String region) {
        return "dashboard:" + accountId.trim() + ":" + region.trim();
    }
}
