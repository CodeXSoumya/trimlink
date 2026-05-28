package com.codex.trimlink.node.kgs;

import java.util.concurrent.atomic.AtomicLong;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.shared.SharedCount;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

@Component
@ConditionalOnBean(CuratorFramework.class)
@ConditionalOnProperty(name = "trimlink.kgs.provider", havingValue = "zookeeper", matchIfMissing = true)
public class ZookeeperRangeKgsClient implements KeyRangeProvider {

    private final long blockSize = 1000;
    private final AtomicLong currentId = new AtomicLong(0);
    private long currentMaxBound = 0;

    // 1. Spring will now inject the global CuratorFramework bean automatically via
    // the constructor
    private final CuratorFramework client;
    private SharedCount globalCounter;

    public ZookeeperRangeKgsClient(CuratorFramework client) {
        this.client = client;
    }

    @PostConstruct
    public void init() throws Exception {
        // 2. The client is already started by CuratorConfig, so we just attach our
        // distributed counter directly!
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
                int currentGlobalValue = globalCounter.getCount();
                int nextGlobalValue = currentGlobalValue + 1;
                if (globalCounter.trySetCount(globalCounter.getVersionedValue(), nextGlobalValue)) {

                    long newStartBound = currentGlobalValue * blockSize + 1;
                    currentMaxBound = newStartBound + blockSize - 1;
                    currentId.set(newStartBound);

                    System.out.printf(
                            "[KGS ZOOKEEPER] Successfully leased cluster range [%d to %d]%n",
                            newStartBound, currentMaxBound);
                    break;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Critical: Failed to lease ID range from ZooKeeper cluster", e);
        }
    }
}