package com.codex.trimlink.node.discovery;

import java.net.InetAddress;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import jakarta.annotation.PostConstruct;

@Configuration
@Profile("!gateway")
public class NodeRegistryLifecycle {

    private final CuratorFramework curatorClient;

    public NodeRegistryLifecycle(CuratorFramework curatorClient) {
        this.curatorClient = curatorClient;
    }

    @PostConstruct
    public void registerNodeInDiscovery() {
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
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
