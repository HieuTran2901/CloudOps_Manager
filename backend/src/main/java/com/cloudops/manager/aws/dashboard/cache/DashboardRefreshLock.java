package com.cloudops.manager.aws.dashboard.cache;

public interface DashboardRefreshLock {
    boolean tryLock(String accountId, String region, long lockTtlSeconds);
    void unlock(String accountId, String region);
    boolean isLocked(String accountId, String region);
}
