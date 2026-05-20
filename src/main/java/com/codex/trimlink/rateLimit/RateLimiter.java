package com.codex.trimlink.rateLimit;

public interface RateLimiter {
	boolean isAllowed(String clientId);
}
