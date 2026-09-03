package com.cloudops.manager.aws.dashboard.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "cloudops.dashboard.cache.type", havingValue = "redis", matchIfMissing = false)
public class RedisDashboardRefreshLock implements DashboardRefreshLock {

    private static final Logger log = LoggerFactory.getLogger(RedisDashboardRefreshLock.class);
    private static final String KEY_PREFIX = "cloudops:dashboard:refresh:";

    private static final String UNLOCK_LUA_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "return redis.call('del', KEYS[1]) " +
            "else " +
            "return 0 " +
            "end";

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> unlockScript;
    private final Map<String, String> activeLockTokens = new ConcurrentHashMap<>();

    public RedisDashboardRefreshLock(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.unlockScript = new DefaultRedisScript<>();
        this.unlockScript.setScriptText(UNLOCK_LUA_SCRIPT);
        this.unlockScript.setResultType(Long.class);
    }

    @Override
    public boolean tryLock(String accountId, String region, long lockTtlSeconds) {
        String key = buildKey(accountId, region);
        String ownerToken = UUID.randomUUID().toString();

        try {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                    key,
                    ownerToken,
                    Duration.ofSeconds(lockTtlSeconds)
            );

            if (Boolean.TRUE.equals(acquired)) {
                activeLockTokens.put(key, ownerToken);
                log.info("REFRESH_LOCK_ACQUIRED (Redis) for key: {}, ttl: {}s", key, lockTtlSeconds);
                return true;
            } else {
                log.info("DASHBOARD_SNAPSHOT_REFRESH_LOCKED (Redis) for key: {}", key);
                return false;
            }
        } catch (Exception e) {
            log.warn("REDIS_UNAVAILABLE during lock acquisition for key: {}. Bypassing lock safely: {}", key, e.getMessage());
            return true; // Safe fallback: allow execution if Redis is unavailable
        }
    }

    @Override
    public void unlock(String accountId, String region) {
        String key = buildKey(accountId, region);
        String ownerToken = activeLockTokens.remove(key);

        if (ownerToken == null) {
            log.warn("Attempted to unlock unowned or expired key: {}", key);
            return;
        }

        try {
            Long result = redisTemplate.execute(unlockScript, Collections.singletonList(key), ownerToken);
            if (Long.valueOf(1).equals(result)) {
                log.info("REFRESH_LOCK_RELEASED (Redis) for key: {}", key);
            } else {
                log.warn("Lock ownership mismatch or expired on release for key: {}", key);
            }
        } catch (Exception e) {
            log.warn("REDIS_UNAVAILABLE during lock release for key: {}", key, e);
        }
    }

    @Override
    public boolean isLocked(String accountId, String region) {
        String key = buildKey(accountId, region);
        try {
            Boolean hasKey = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(hasKey);
        } catch (Exception e) {
            log.warn("REDIS_UNAVAILABLE on isLocked for key: {}", key, e);
            return false;
        }
    }

    private String buildKey(String accountId, String region) {
        return KEY_PREFIX + accountId.trim() + ":" + region.trim();
    }
}
