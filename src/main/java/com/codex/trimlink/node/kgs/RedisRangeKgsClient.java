package com.codex.trimlink.node.kgs;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "trimlink.kgs.provider", havingValue = "redis")
public class RedisRangeKgsClient implements KeyRangeProvider {

    private static final String GLOBAL_KGS_COUNTER_KEY = "trimlink:kgs:global-counter";

    private final StringRedisTemplate redisTemplate;
    private final long blockSize;
    private final AtomicLong currentId = new AtomicLong(0);
    private volatile long currentMaxBound = 0;

    public RedisRangeKgsClient(
            StringRedisTemplate redisTemplate,
            @org.springframework.beans.factory.annotation.Value("${trimlink.kgs.block-size:1000}") long blockSize) {
        this.redisTemplate = redisTemplate;
        this.blockSize = blockSize;
    }

    @Override
    public synchronized long getNextId() {
        if (currentId.get() >= currentMaxBound) {
            leaseNewBlockFromRedis();
        }
        return currentId.getAndIncrement();
    }

    private void leaseNewBlockFromRedis() {
        Long newGlobalEnd = redisTemplate.opsForValue().increment(GLOBAL_KGS_COUNTER_KEY, blockSize);
        if (newGlobalEnd == null) {
            throw new IllegalStateException("Failed to lease ID range from Redis-backed KGS counter");
        }

        long newStartBound = newGlobalEnd - blockSize + 1;
        currentMaxBound = newGlobalEnd + 1;
        currentId.set(newStartBound);

        System.out.printf("[KGS REDIS] Successfully leased cluster range [%d to %d]%n", newStartBound, newGlobalEnd);
    }
}
