package com.codex.trimlink.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.codex.trimlink.rateLimit.*;
import com.codex.trimlink.hashing.*;

@Configuration
public class GatewayConfig {
    
    @Bean
    public RateLimiter rateLimiter() {
        // Global limit: 5 requests per minute
        return new InMemorySlidingWindowRateLimiter(60000, 5);
    }

    @Bean
    public ConsistentHashingStrategy hashingStrategy() {
        // 200 virtual nodes for better distribution across 10 real nodes
        return new Murmur3Strategy(200, Arrays.asList(
            "node-1", "node-2", "node-3", "node-4", "node-5",
            "node-6", "node-7", "node-8", "node-9", "node-10"
        ));
    }
}
