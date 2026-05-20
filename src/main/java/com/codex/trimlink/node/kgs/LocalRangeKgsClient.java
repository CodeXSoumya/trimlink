package com.codex.trimlink.node.kgs;

import java.util.concurrent.atomic.AtomicLong;

public class LocalRangeKgsClient implements KeyRangeProvider {
    
    private final long blockSize = 1000; // Each lease provides 1000 IDs
    private final AtomicLong currentId = new AtomicLong(0);
    private long currentMaxBound = 0L; 

    @Override
    public synchronized long getNextId() {
        if (currentId.get() >= currentMaxBound) {
            leaseNewBlockFromKgs();
        }
        return currentId.getAndIncrement();
    }

    private void leaseNewBlockFromKgs() {
        // MOCK LOGIC: In the real implementation, this is supposed make a call to the central KGS service
        System.out.println("[KGS Client] Current block depleted. Fetching new range from central KGS...");
        
        // Simulating the central KGS shifting its global pointer forward
        long newStartBound = currentMaxBound + 1;
        currentMaxBound = newStartBound + blockSize - 1;
        currentId.set(newStartBound);

        System.out.printf("[KGS Client] Successfully leased range [%d to %d]%n", newStartBound, currentMaxBound);
    }
    
}
