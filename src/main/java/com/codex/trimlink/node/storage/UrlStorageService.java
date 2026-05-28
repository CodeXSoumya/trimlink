package com.codex.trimlink.node.storage;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class UrlStorageService {

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

        // Write to Cache: Warm-up (Global shared cache layer across nodes)
        redisTemplate.opsForValue().set(shortCode, longUrl, 24, TimeUnit.HOURS);
    }

    public String getLongUrl(String shortCode) {
        // Query the ultra-fast RAM Cache
        String cachedData = redisTemplate.opsForValue().get(shortCode);
        if (cachedData != null) { // Cache HIT
            return cachedData;
        }

        // Cache MISS! -> Fall back to the persistent Relational Database

        // MUTEX lock(Distributed SETNX Lock) to avoid Cache Stampede when multiple
        // threads try to fetch the same short code that is not in cache
        String lockKey = "lock:" + shortCode;
        // Attempt to acquire the lock with a timeout to prevent deadlocks
        Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "locked", 10, TimeUnit.SECONDS);

        // If lock is acquired, this thread is responsible for fetching from DB and
        // updating cache.
        if (Boolean.TRUE.equals(lockAcquired)) {
            try {
                // Double-checked locking style: If a thread has already fetched the long url
                // and saved in cache subsequently
                cachedData = redisTemplate.opsForValue().get(shortCode);
                if (cachedData != null)
                    return cachedData;

                String longUrl;
                try {
                    // CRITICAL STEP: Bind this worker thread context to the specific database shard
                    // signature
                    ShardContext.setShardKey(shortCode);

                    // CRITICAL SECTION: Only one thread should make the call to the Persistent DB
                    // Shard
                    Optional<UrlMapping> dbMapping = dbMappingRepository.findByShortCode(shortCode);
                    if (!dbMapping.isPresent()) {
                        return null; // No long url found!!!
                    }
                    longUrl = dbMapping.get().getLongUrl();

                } finally {
                    // Safely clean up thread local map context state right after data mapping
                    // retrieval
                    ShardContext.clear();
                }

                // Save in Cache -> for sub-ms fetch for the next 24 hours if queried
                redisTemplate.opsForValue().set(shortCode, longUrl, 24, TimeUnit.HOURS);
                // TTL(Cache Invalidation): 24 hours to keep Redis clean from dead URLs

                return longUrl;
            } finally {
                // Release the lock so that other threads can fetch from DB if needed
                redisTemplate.delete(lockKey);
            }
        } else { // Otherwise some other thread is already fetching the long url and updating
                 // cache, so we wait for a short duration and then try fetching from cache again
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return getLongUrl(shortCode); // Retry fetching from cache after some delay
        }
    }
}