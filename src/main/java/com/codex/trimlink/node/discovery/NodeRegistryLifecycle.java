package com.codex.trimlink.node.discovery;

import java.net.InetAddress;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import jakarta.annotation.PostConstruct;

@Configuration
@Profile("!gateway")
@ConditionalOnBean(CuratorFramework.class)
public class NodeRegistryLifecycle {

    private final CuratorFramework curatorClient;

    @Value("${TRIMLINK_NODE_ADVERTISED_HOST:}")
    private String advertisedHost;

    public NodeRegistryLifecycle(CuratorFramework curatorClient) {
        this.curatorClient = curatorClient;
    }

    @PostConstruct
    public void registerNodeInDiscovery() {
        try {
            String hostname = advertisedHost;
            if (hostname == null || hostname.isBlank()) {
                hostname = InetAddress.getLocalHost().getHostName();
            }
            String registrationPath = "/registry/nodes/" + hostname;

            if (curatorClient.checkExists().forPath(registrationPath) != null) {
                curatorClient.delete().forPath(registrationPath);
            }

            curatorClient.create()
                    .creatingParentsIfNeeded()
                    .withMode(CreateMode.EPHEMERAL)
                    .forPath(registrationPath, hostname.getBytes());

            System.out.printf("[ZK SERVICE REGISTRY] Node [%s] successfully checked in as live cluster member.%n",
                    hostname);
        } catch (Exception e) {
            System.err.println(
                    "Critical exception attempting to register node to ZooKeeper directory: " + e.getMessage());
        }
    }

}
