package com.codex.trimlink.config;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CuratorConfig {

    @Value("${ZOOKEEPER_SERVER:zookeeper-service:2181}")
    private String zookeeperAddress;

    @Bean(initMethod = "start", destroyMethod = "close")
    public CuratorFramework curatorFramework() {
        ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(1000, 3);

        System.out.printf("[INFRASTRUCTURE] Initializing Single Source of Truth Curator Client -> [%s]%n",
                zookeeperAddress);

        return CuratorFrameworkFactory.builder()
                .connectString(zookeeperAddress)
                .sessionTimeoutMs(60000)
                .connectionTimeoutMs(15000)
                .retryPolicy(retryPolicy)
                .build();
    }
}