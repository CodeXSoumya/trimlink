package com.codex.trimlink.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import org.springframework.data.redis.core.StringRedisTemplate;

import com.codex.trimlink.gateway.RateLimitingFilter;
import com.codex.trimlink.rateLimit.RedisSlidingWindowRateLimiter;
import com.codex.trimlink.rateLimit.RateLimiter;

@Configuration
@Profile("gateway") // Ensures this entire config file ONLY initializes on the API Gateway
public class GatewayConfig {

    @Bean
    public RateLimiter rateLimiter(StringRedisTemplate redisTemplate) {
        // Global limit: 100 requests per minute using clustered Redis rate limiting
        System.out.println("[GATEWAY CONFIG] Initializing Redis-based sliding window rate limiter (100 req/min).");
        return new RedisSlidingWindowRateLimiter(redisTemplate, 60000, 100);
    }

    @Bean
    public FilterRegistrationBean<RateLimitingFilter> loggingFilter(RateLimiter rateLimiter) {
        FilterRegistrationBean<RateLimitingFilter> registrationBean = new FilterRegistrationBean<>();

        // Explicitly instantiate and wire the filter with the gateway's rate limiter
        // bean
        registrationBean.setFilter(new RateLimitingFilter(rateLimiter));

        // Apply this filter exclusively to the public API ingress path routes
        registrationBean.addUrlPatterns("/api/v1/*");

        // Ensure it executes at the very front of the request chain pipeline
        registrationBean.setOrder(1);

        System.out.println("[GATEWAY CONFIG] RateLimitingFilter successfully attached to API edge ingress path.");
        return registrationBean;
    }
}