package com.codex.trimlink.gateway;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.curator.framework.CuratorFramework;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/v1")
public class GatewayProxyController {

    private final CuratorFramework curatorClient;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${TRIMLINK_NODE_SCHEME:http}")
    private String nodeScheme;

    @Value("${TRIMLINK_NODE_PORT:8080}")
    private String nodePort;

    @Value("${TRIMLINK_GATEWAY_STATIC_NODES:}")
    private String staticNodesCsv;

    private final AtomicInteger roundRobinCounter = new AtomicInteger(0);

    public GatewayProxyController(ObjectProvider<CuratorFramework> curatorClientProvider) {
        this.curatorClient = curatorClientProvider.getIfAvailable();
    }

    @PostMapping("/shorten")
    public ResponseEntity<String> proxyShortenRequest(@RequestParam String longUrl) {
        try {
            List<String> activeContainerHosts = resolveActiveNodes();

            if (activeContainerHosts == null || activeContainerHosts.isEmpty()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body("Gateway Route Failure: Zero active compute nodes detected in cluster");
            }

            int currentSequenceValue = roundRobinCounter.getAndIncrement();
            int targetingIndex = Math.abs(currentSequenceValue) % activeContainerHosts.size();
            String assignedHost = activeContainerHosts.get(targetingIndex);

            String proxyDestinationUrl = nodeScheme + "://" + assignedHost + ":" + nodePort + "/internal/shorten?longUrl=" + longUrl;

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
            List<String> activeContainerHosts = resolveActiveNodes();
            if (activeContainerHosts == null || activeContainerHosts.isEmpty()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
            }

            int currentSequenceValue = roundRobinCounter.getAndIncrement();
            int targetingIndex = Math.abs(currentSequenceValue) % activeContainerHosts.size();
            String assignedHost = activeContainerHosts.get(targetingIndex);

            String proxyDestinationUrl = nodeScheme + "://" + assignedHost + ":" + nodePort + "/internal/resolve/" + shortCode;

            // Forward the lookup query to the selected node
            return restTemplate.getForEntity(proxyDestinationUrl, Void.class);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private List<String> resolveActiveNodes() throws Exception {
        if (staticNodesCsv != null && !staticNodesCsv.isBlank()) {
            return Arrays.stream(staticNodesCsv.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toList());
        }

        if (curatorClient != null) {
            return curatorClient.getChildren().forPath("/registry/nodes");
        }

        return Collections.emptyList();
    }
}
