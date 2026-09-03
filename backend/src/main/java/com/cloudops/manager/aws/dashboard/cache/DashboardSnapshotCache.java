package com.cloudops.manager.aws.dashboard.cache;

import com.cloudops.manager.aws.dashboard.model.DashboardSnapshot;

import java.util.Optional;

public interface DashboardSnapshotCache {
    Optional<DashboardSnapshot> get(String accountId, String region);
    void put(DashboardSnapshot snapshot);
    void invalidate(String accountId, String region);
    boolean exists(String accountId, String region);
}
