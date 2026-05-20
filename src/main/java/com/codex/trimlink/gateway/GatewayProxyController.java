package com.codex.trimlink.gateway;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import com.codex.trimlink.hashing.ConsistentHashingStrategy;

@RestController
@RequestMapping("/api/v1")
public class GatewayProxyController {
    
    private final ConsistentHashingStrategy hashRing;
    private final RestTemplate restTemplate = new RestTemplate();

    public GatewayProxyController(ConsistentHashingStrategy hashRing) {
        this.hashRing = hashRing;
    }

    @PostMapping("/shorten")
    public ResponseEntity<String> proxyShortenRequest(@RequestParam String longUrl) {
        // Consistent hashing to determine data partition ownership
        String targetNodeHost = hashRing.get(longUrl);

        // Construct the target internal address
        String targetUrl = String.format(
            "http://%s:8080/internal/shorten?longUrl=%s",
            targetNodeHost, longUrl
        );

        try {
            // Forward the payload down to the correct node instance
            return restTemplate.postForEntity(targetUrl, null, String.class);
        } catch (Exception e) {
            // Handle exceptions (e.g., node failure, network issues)
            return ResponseEntity.status(503).body("Target Partition Unavailable: " + e.getMessage());
        }   
    }
}
