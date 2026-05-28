package com.codex.trimlink.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

import com.codex.trimlink.node.storage.UrlStorageService;

@RestController
@RequestMapping("/api/v1")
public class UrlRedirectController {

    private final UrlStorageService urlStorageService;

    public UrlRedirectController(UrlStorageService urlStorageService) {
        this.urlStorageService = urlStorageService;
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirectToLongUrl(@PathVariable String shortCode) {
        // urlStorageService natively handles Redis lookups, distributed locks,
        // sharded database context routing, and cache hydration.
        String targetLongUrl = urlStorageService.getLongUrl(shortCode);

        if (targetLongUrl == null) {
            System.out.printf("[REDIRECT FAILED] Token [%s] lookup failed.%n", shortCode);
            return ResponseEntity.notFound().build();
        }

        System.out.printf("[REDIRECT SUCCESS] Redirecting [%s] -> %s%n", shortCode, targetLongUrl);

        String redirectUrl = normalizeRedirectUrl(targetLongUrl);

        // HTTP 302 Found to redirect the browser client
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
