package com.codex.trimlink.rateLimit;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;

public class RedisSlidingWindowRateLimiter implements RateLimiter {

    private final StringRedisTemplate redisTemplate;
    private final long windowSizeMs;
    private final int maxLimit;

    public RedisSlidingWindowRateLimiter(StringRedisTemplate redisTemplate, long windowSizeMs, int maxLimit) {
        this.redisTemplate = redisTemplate;
        this.windowSizeMs = windowSizeMs;
        this.maxLimit = maxLimit;
    }

    @Override
    public boolean isAllowed(String clientId) {
        String key = "rate_limit:" + clientId;
        long currentTime = System.currentTimeMillis();
        long windowStartBound = currentTime - windowSizeMs;

        try {
            // Remove expired entries older than the sliding window bounds
            redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStartBound);
            
            // Get current count of active entries in the window
            Long count = redisTemplate.opsForZSet().zCard(key);
            
            if (count != null && count < maxLimit) {
                // Use random UUID prefix to ensure simultaneous requests don't overwrite each other
                String uniqueMember = currentTime + ":" + UUID.randomUUID().toString();
                redisTemplate.opsForZSet().add(key, uniqueMember, currentTime);
                // Set sliding expiration slightly larger than the window size to auto-clean up idle keys
                redisTemplate.expire(key, windowSizeMs + 5000, TimeUnit.MILLISECONDS);
                return true;
            }
            return false;
        } catch (Exception e) {
            // Fail-open strategy to ensure rate-limiting issues don't bring down user services
            System.err.println("[REDIS RATE LIMITER] Fail-open: Redis check failure -> " + e.getMessage());
            return true;
        }
    }
}
