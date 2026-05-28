package com.codex.trimlink.gateway;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.curator.framework.CuratorFramework;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/v1")
public class GatewayProxyController {

    private final CuratorFramework curatorClient;
    private final RestTemplate restTemplate = new RestTemplate();

    private final AtomicInteger roundRobinCounter = new AtomicInteger(0);

    public GatewayProxyController(CuratorFramework curatorClient) {
        this.curatorClient = curatorClient;
    }

    @PostMapping("/shorten")
    public ResponseEntity<String> proxyShortenRequest(@RequestParam String longUrl) {
        try {
            String nodesDirectoryPath = "/registry/nodes";
            List<String> activeContainerHosts = curatorClient.getChildren().forPath(nodesDirectoryPath);

            if (activeContainerHosts == null || activeContainerHosts.isEmpty()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body("Gateway Route Failure: Zero active compute nodes detected in cluster");
            }

            int currentSequenceValue = roundRobinCounter.getAndIncrement();
            int targetingIndex = Math.abs(currentSequenceValue) % activeContainerHosts.size();
            String assignedHost = activeContainerHosts.get(targetingIndex);

            String proxyDestinationUrl = "http://" + assignedHost + ":8080/internal/shorten?longUrl=" + longUrl;

            System.out.printf("[GATEWAY BALANCER] Dispatched shortening sequence #%d to target container -> %s%n",
                    currentSequenceValue, proxyDestinationUrl);

            String generatedShortCode = restTemplate.postForObject(proxyDestinationUrl, null, String.class);
            return ResponseEntity.ok(generatedShortCode);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Gateway Proxy Routing Interruption Error: " + e.getMessage());
        }
    }

    @GetMapping("/resolve/{shortCode}")
    public ResponseEntity<Void> proxyResolve(@PathVariable String shortCode) {
        try {
            // Read lookups follow the same dynamic round-robin sequence to completely
            // spread execution load
            List<String> activeContainerHosts = curatorClient.getChildren().forPath("/registry/nodes");
            if (activeContainerHosts == null || activeContainerHosts.isEmpty()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
            }

            int currentSequenceValue = roundRobinCounter.getAndIncrement();
            int targetingIndex = Math.abs(currentSequenceValue) % activeContainerHosts.size();
            String assignedHost = activeContainerHosts.get(targetingIndex);

            String proxyDestinationUrl = "http://" + assignedHost + ":8080/internal/resolve/" + shortCode;

            // Forward the lookup query to the selected node
            return restTemplate.getForEntity(proxyDestinationUrl, Void.class);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
