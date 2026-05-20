package com.codex.trimlink.rateLimit;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemorySlidingWindowRateLimiter implements RateLimiter {
	
	// GRANULAR LOCKING
	// ConcurrentHashMap: Allows safe, concurrent reads and updates across multiple threads without locking the entire map.
	// CopyOnWriteArrayList/synchronized list collection: Ensures that while one thread is pruning expired timestamps, another thread can safely read or add to the list.
	
	private final Map<String, List<Long>> storage = new ConcurrentHashMap<>(); // clientId -> {timestamp_1, timestamp_2, ... }
	
	private final long windowSizeMs;
	private final int MAX_LIMIT;
	
	public InMemorySlidingWindowRateLimiter(long windowSizeMs, int maxLimit) {
		this.windowSizeMs = windowSizeMs;
		this.MAX_LIMIT = maxLimit;
	}
	
	/**
     * Evaluates if a client request is within their allowed threshold.
     * Synchronized "per client identifier" to prevent race conditions on the timestamp list.
     */
	public boolean isAllowed(String clientId) {
		long currentTime = System.currentTimeMillis();
		long windowStartBound = currentTime - windowSizeMs;
		
		// Thread-safe initialization of the client's timestamp list
		List<Long> timestamps = storage.computeIfAbsent(clientId, k -> new CopyOnWriteArrayList<>());
		
		// Granular Locking: Lock specifically on THIS client's list to make the check-and-modify step atomic
		synchronized (timestamps) {
			// Remove expired timestamps outside the time window
            // Using removeIf because CopyOnWriteArrayList's iterator safely supports it
			timestamps.removeIf(timestamp -> timestamp < windowStartBound);
			
			// Check the number of remaining requests post-filtration
			if (timestamps.size() < MAX_LIMIT) {
				timestamps.add(currentTime); // add the current timestamp
				return true; // ALLOWED
			}
			return false; // REJECTED
		}
	}
	
	// --- Simulation and Validation ---
	/*
    public static void main(String[] args) throws InterruptedException {
        // Limit: Max 3 requests within a rolling 2-second window
        InMemorySlidingWindowRateLimiter rateLimiter = new InMemorySlidingWindowRateLimiter(2000, 5);
        String client = "client_1";

        System.out.println("--- Executing burst requests ---");
        for (int i = 1; i <= 7; i++) {
            if (rateLimiter.isAllowed(client)) {
                System.out.printf("Request %d: 200 OK%n", i);
            } else {
                System.out.printf("Request %d: Too Many Requests: Try again later.%n", i);
            }
            Thread.sleep(200); // 200ms gap between requests
        }

        System.out.println("\n--- Waiting 1.5 seconds for window to partially decay ---");
        Thread.sleep(1500);

        System.out.println("--- Executing subsequent requests ---");
        for (int i = 8; i <= 10; i++) {
            if (rateLimiter.isAllowed(client)) {
                System.out.printf("Request %d: 200 OK%n", i);
            } else {
                System.out.printf("Request %d: Too Many Requests: Try again later.%n", i);
            }
        }
    }
    */
}
