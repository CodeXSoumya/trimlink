package com.codex.trimlink.node.storage;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class UrlStorageService {

    private static final Logger log = LoggerFactory.getLogger(UrlStorageService.class);

    private final UrlMappingRepository dbMappingRepository;
    private final StringRedisTemplate redisTemplate;

    public UrlStorageService(UrlMappingRepository dbMappingRepository, StringRedisTemplate redisTemplate) {
        this.dbMappingRepository = dbMappingRepository;
        this.redisTemplate = redisTemplate;
    }

    public void saveMapping(Long id, String shortCode, String longUrl) {
        try {
            // CRITICAL STEP: Set the sharding key flag on the current worker execution
            // thread.
            // This enables ShardedRoutingDataSource to intercept the transaction and pick
            // the correct PostgreSQL database instance.
            ShardContext.setShardKey(shortCode);

            // Write to DB first (Source-of-Truth transaction) targeting the computed
            // database shard
            dbMappingRepository.save(new UrlMapping(id, shortCode, longUrl));

        } finally {
            // Always scrub the ThreadLocal token clean to prevent memory leaks or
            // context-bleeding inside Tomcat's reusable thread pool
            ShardContext.clear();
        }

        // Cache warm-up is best-effort. DB is the source of truth and should not fail due
        // to transient cache issues.
        try {
            redisTemplate.opsForValue().set(shortCode, longUrl, 24, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("[CACHE WRITE SKIPPED] Failed to warm cache for shortCode={}. Cause: {}", shortCode,
                    e.getMessage());
        }
    }

    public String getLongUrl(String shortCode) {
        String cachedData = null;
        try {
            // Query the ultra-fast RAM Cache
            cachedData = redisTemplate.opsForValue().get(shortCode);
            if (cachedData != null) { // Cache HIT
                return cachedData;
            }
        } catch (Exception e) {
            log.warn("[CACHE READ SKIPPED] Redis unavailable for shortCode={}. Falling back to DB. Cause: {}",
                    shortCode, e.getMessage());
            return readFromDbAndOptionallyPopulateCache(shortCode, false);
        }

        // Cache MISS! -> Fall back to the persistent Relational Database

        // MUTEX lock(Distributed SETNX Lock) to avoid Cache Stampede when multiple
        // threads try to fetch the same short code that is not in cache
        String lockKey = "lock:" + shortCode;
        // Attempt to acquire the lock with a timeout to prevent deadlocks
        Boolean lockAcquired;
        try {
            lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "locked", 10, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("[CACHE LOCK SKIPPED] Failed to acquire Redis lock for shortCode={}. Falling back to DB. Cause: {}",
                    shortCode, e.getMessage());
            return readFromDbAndOptionallyPopulateCache(shortCode, false);
        }

        // If lock is acquired, this thread is responsible for fetching from DB and
        // updating cache.
        if (Boolean.TRUE.equals(lockAcquired)) {
            try {
                // Double-checked locking style: If a thread has already fetched the long url
                // and saved in cache subsequently
                try {
                    cachedData = redisTemplate.opsForValue().get(shortCode);
                } catch (Exception e) {
                    log.warn("[CACHE DOUBLE-CHECK SKIPPED] Redis read failed for shortCode={}. Cause: {}", shortCode,
                            e.getMessage());
                }
                if (cachedData != null)
                    return cachedData;

                return readFromDbAndOptionallyPopulateCache(shortCode, true);
            } finally {
                // Release the lock so that other threads can fetch from DB if needed
                try {
                    redisTemplate.delete(lockKey);
                } catch (Exception e) {
                    log.warn("[CACHE LOCK RELEASE SKIPPED] Failed to release lock for shortCode={}. Cause: {}",
                            shortCode, e.getMessage());
                }
            }
        } else { // Otherwise some other thread is already fetching the long url and updating
                 // cache, so we wait for a short duration and then try fetching from cache again
            for (int attempt = 0; attempt < 5; attempt++) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                try {
                    cachedData = redisTemplate.opsForValue().get(shortCode);
                    if (cachedData != null) {
                        return cachedData;
                    }
                } catch (Exception e) {
                    log.warn("[CACHE RETRY SKIPPED] Redis retry read failed for shortCode={}. Cause: {}", shortCode,
                            e.getMessage());
                    break;
                }
            }

            return readFromDbAndOptionallyPopulateCache(shortCode, false);
        }
    }

    private String readFromDbAndOptionallyPopulateCache(String shortCode, boolean populateCache) {
        String longUrl;
        try {
            ShardContext.setShardKey(shortCode);
            Optional<UrlMapping> dbMapping = dbMappingRepository.findByShortCode(shortCode);
            if (!dbMapping.isPresent()) {
                return null;
            }
            longUrl = dbMapping.get().getLongUrl();
        } finally {
            ShardContext.clear();
        }

        if (populateCache) {
            try {
                redisTemplate.opsForValue().set(shortCode, longUrl, 24, TimeUnit.HOURS);
            } catch (Exception e) {
                log.warn("[CACHE POPULATE SKIPPED] Failed to cache shortCode={}. Cause: {}", shortCode,
                        e.getMessage());
            }
        }

        return longUrl;
    }
}