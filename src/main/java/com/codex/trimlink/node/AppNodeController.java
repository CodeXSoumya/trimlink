package com.codex.trimlink.node;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.codex.trimlink.node.kgs.KeyRangeProvider;
import com.codex.trimlink.node.storage.UrlStorageService;
import com.codex.trimlink.utils.Base62Encoder;

@RestController
@RequestMapping("/internal")
public class AppNodeController {
    
    @Value("${HOSTNAME:unknown-node}")
    private String nodeName;

    private final KeyRangeProvider kgsClient;
    private final UrlStorageService urlStorageService;

    public AppNodeController(UrlStorageService urlStorageService, KeyRangeProvider kgsClient) {
        this.urlStorageService = urlStorageService;
        this.kgsClient = kgsClient;
    }

    /**
     * WRITE: Generates a short URL mapping and updates DB + Cache.
     */
    @PostMapping("/shorten")
    public ResponseEntity<String> shortenUrl(@RequestParam String longUrl) {
        // Lease a Golbally Unique ID (base-10)
        long uniqueId = kgsClient.getNextId();

        // Compress the long ID into a short 6-7 character token
        String shortCode = Base62Encoder.encode(uniqueId);

        // Persist shortCode -> longUrl to the Distributed DB/Redis Cluster
        urlStorageService.saveMapping(uniqueId, shortCode, longUrl);

        System.out.printf("[NODE WRITE] Node [%s] shortended URL to code [%s] (Database ID: %d)",
            nodeName, shortCode, uniqueId
        );

        return ResponseEntity.ok(shortCode);
    }

    /**
     * READ: Resolves a short code to its long URL destination.
     */
    @GetMapping("/resolve/{shortCode}")
    public ResponseEntity<Void> resolveUrl(@PathVariable String shortCode) {
        // Resolve the long URL
        String targetLongUrl = urlStorageService.getLongUrl(shortCode);

        if (targetLongUrl == null) {
            System.out.printf("[NODE READ] Node [%s] lookup failed", nodeName);
            return ResponseEntity.notFound().build();
        }

        System.out.printf("[NODE READ] Node [%s] lookup is successful. [%s] redirects to [%s]", nodeName, shortCode, targetLongUrl);

        String redirectUrl = normalizeRedirectUrl(targetLongUrl);
        
        // HTTP 302 Found (Temporary Redirect) to send the browser to the long URL
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirectUrl))
                .build();
    }

    private String normalizeRedirectUrl(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        if (trimmed.matches("^[a-zA-Z][a-zA-Z\\d+\\-.]*://.*")) {
            return trimmed;
        }

        return "https://" + trimmed;
    }
}
