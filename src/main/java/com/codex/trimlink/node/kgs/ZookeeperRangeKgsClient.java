package com.codex.trimlink.node.kgs;

import java.util.concurrent.atomic.AtomicLong;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.shared.SharedCount;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class ZookeeperRangeKgsClient implements KeyRangeProvider {

    @Value("${ZOOKEEPER_SERVER:zookeeper-service:2181}")
    private String zkConnectString;

    private final long blockSize = 1000;
    private final AtomicLong currentId = new AtomicLong(0);
    private long currentMaxBound = 0;

    private CuratorFramework client;
    private SharedCount globalCounter;

    @PostConstruct
    public void init() throws Exception {
        // Establish a resilient connection to the ZooKeeper Cluster
        client = CuratorFrameworkFactory.newClient(
            zkConnectString, 
            new ExponentialBackoffRetry(1000, 3)
        );
        client.start();

        // Connect to a persistent distributed counter node 
        globalCounter = new SharedCount(client, "/kgs_global_sequence", 0);
        globalCounter.start();
    }

    @Override
    public synchronized long getNextId() {
        if (currentId.get() >= currentMaxBound) {
            leaseNewBlockFromZookeeper();
        }
        return currentId.getAndIncrement();
    }
    
    private void leaseNewBlockFromZookeeper() {
        System.out.println("[KGS ZOOKEEPER] Local block depleted. Atomic Leasing from Cluster...");
        try {
            while (true) {
                // Read the current global counter value from ZooKeeper quorum memory
                int currentGlobalValue = globalCounter.getCount();
                int nextGlobalValue = currentGlobalValue + 1;
                if (globalCounter.trySetCount(globalCounter.getVersionedValue(), nextGlobalValue)) {

                    long newStartBound = currentGlobalValue * blockSize + 1;
                    currentMaxBound = newStartBound + blockSize - 1;
                    currentId.set(newStartBound);

                    System.out.printf(
                        "[KGS ZOOKEEPER] Successfully leased cluster range [%d to %d]%n",
                        newStartBound, currentMaxBound
                    );
                    break;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Critical: Failed to lease ID range from ZooKeeper cluster", e);
        }
    }
}
